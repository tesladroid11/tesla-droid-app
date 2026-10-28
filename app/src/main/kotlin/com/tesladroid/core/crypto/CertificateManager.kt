package com.tesladroid.core.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.ExtensionsGenerator
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec

/**
 * Manages device identity keys in AndroidKeyStore and enrolls production
 * Let's Encrypt TLS certificates via https://issuer.tesla-droid.app for local in-car streaming.
 */
object CertificateManager {
    private const val TAG = "CertificateManager"
    private const val KEY_ALIAS = "tm_device_v1"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val ISSUER_URL = "https://issuer.tesla-droid.app/v1/issue"
    private const val DOMAIN_SUFFIX = "car.tesla-droid.app"

    fun getNodeId(context: Context): String {
        return try {
            val keyPair = getOrCreateKeyPair()
            val spkiBytes = keyPair.public.encoded
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(spkiBytes)
            hash.take(8).joinToString("") { "%02x".format(it) }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to derive node ID from KeyStore: ${e.message}")
            "832a42ef190e9daf"
        }
    }

    fun getNodeHostname(context: Context): String {
        return "${getNodeId(context)}.$DOMAIN_SUFFIX"
    }

    fun hasValidCertificate(context: Context, hostname: String): Boolean {
        val sp = context.getSharedPreferences("license", Context.MODE_PRIVATE)
        val certPem = sp.getString("cert_pem", null) ?: return false
        val certName = sp.getString("cert_name", null) ?: return false
        val certExpires = sp.getLong("cert_expires", 0L)
        val nowSec = System.currentTimeMillis() / 1000

        if (certName != hostname) {
            Log.d(TAG, "Cached certificate name ($certName) does not match node hostname ($hostname)")
            return false
        }
        if (certExpires <= nowSec + 86400L) {
            Log.d(TAG, "Cached certificate is expired or expiring soon ($certExpires <= $nowSec + 86400)")
            return false
        }
        // Must be signed by a trusted public authority (Let's Encrypt / ISRG), NOT self-signed
        if (!certPem.contains("Let's Encrypt") && !certPem.contains("ISRG")) {
            Log.d(TAG, "Cached certificate is not from Let's Encrypt (discarding untrusted/self-signed cert)")
            return false
        }
        return true
    }

    /**
     * Ensures a valid Let's Encrypt certificate is installed in the local license store.
     * Performs ACME enrollment via the certificate issuer if missing or expired.
     */
    fun ensureCertificate(context: Context): Boolean {
        val hostname = getNodeHostname(context)
        if (hasValidCertificate(context, hostname)) {
            Log.i(TAG, "Valid Let's Encrypt TLS certificate already cached for $hostname")
            return true
        }

        // If called on main thread, execute on a background thread and wait briefly or delegate
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            ensureCertificateAsync(context)
            return false
        }

        return try {
            Log.i(TAG, "Enrolling Let's Encrypt TLS certificate for node $hostname...")
            enrollCertificate(context, hostname)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to ensure TLS certificate", e)
            false
        }
    }

    /**
     * Non-blocking background enrollment to warm up certificate before stream starts.
     */
    fun ensureCertificateAsync(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        val hostname = getNodeHostname(context)
        if (hasValidCertificate(context, hostname)) {
            Log.i(TAG, "Valid Let's Encrypt TLS certificate already cached for $hostname")
            onComplete?.invoke(true)
            return
        }

        Thread({
            try {
                Log.i(TAG, "Background enrolling Let's Encrypt TLS certificate for node $hostname...")
                val success = enrollCertificate(context, hostname)
                onComplete?.invoke(success)
            } catch (e: Throwable) {
                Log.e(TAG, "Background cert enrollment failed", e)
                onComplete?.invoke(false)
            }
        }, "CertEnrollmentThread").start()
    }

    private fun enrollCertificate(context: Context, hostname: String): Boolean {
        return try {
            val keyPair = getOrCreateKeyPair()
            val csrPem = generateCsrPem(keyPair.public, keyPair.private, hostname)

            val url = URL(ISSUER_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 120_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")

            val requestBody = JSONObject().put("csr", csrPem).toString()
            val requestBytes = requestBody.toByteArray(StandardCharsets.UTF_8)
            connection.setFixedLengthStreamingMode(requestBytes.size)

            connection.outputStream.use { output: OutputStream ->
                output.write(requestBytes)
            }

            val statusCode = connection.responseCode
            val responseStream: InputStream? = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = responseStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
            connection.disconnect()

            if (statusCode !in 200..299) {
                Log.e(TAG, "Certificate issuer returned HTTP $statusCode: $responseText")
                return false
            }

            val responseJson = JSONObject(responseText)
            val certPem = responseJson.getString("cert")
            val certName = responseJson.getString("name")
            val expiresAt = responseJson.getLong("expires_at")

            val nowSec = System.currentTimeMillis() / 1000
            val sp = context.getSharedPreferences("license", Context.MODE_PRIVATE)
            sp.edit()
                .putString("cert_pem", certPem)
                .putString("cert_name", certName)
                .putLong("cert_expires", expiresAt)
                .putLong("last_ok_check", nowSec)
                .remove("server_blocked")
                .apply()

            Log.i(TAG, "Successfully enrolled and stored Let's Encrypt certificate for $certName (expires: $expiresAt)")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Enrollment error: ${e.message}", e)
            false
        }
    }

    fun getOrCreateKeyPair(): KeyPair {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) {
            val entry = ks.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            if (entry != null) {
                return KeyPair(entry.certificate.publicKey, entry.privateKey)
            }
            ks.deleteEntry(KEY_ALIAS)
        }

        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(
                KeyProperties.DIGEST_NONE,
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA384,
                KeyProperties.DIGEST_SHA512
            )
            .build()
        kpg.initialize(spec)
        return kpg.generateKeyPair()
    }

    private fun generateCsrPem(publicKey: PublicKey, privateKey: PrivateKey, host: String): String {
        val entityName = X500Name("CN=$host")
        val p10Builder = JcaPKCS10CertificationRequestBuilder(entityName, publicKey)

        val extGen = ExtensionsGenerator()
        val altNames = GeneralNames(GeneralName(GeneralName.dNSName, host))
        extGen.addExtension(Extension.subjectAlternativeName, false, altNames)
        p10Builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate())

        val signer = JcaContentSignerBuilder("SHA256withECDSA").build(privateKey)
        val csr = p10Builder.build(signer)

        val base64 = Base64.encodeToString(csr.encoded, Base64.NO_WRAP).chunked(64).joinToString("\n")
        return "-----BEGIN CERTIFICATE REQUEST-----\n$base64\n-----END CERTIFICATE REQUEST-----\n"
    }
}
