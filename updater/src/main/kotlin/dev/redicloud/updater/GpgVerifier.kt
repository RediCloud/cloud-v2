package dev.redicloud.updater

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSignatureList
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider
import java.io.ByteArrayInputStream
import java.security.Security

/**
 * Verifies detached GPG (ASCII-armored) signatures using Bouncy Castle.
 *
 * The trusted public key is embedded as a classpath resource (`trusted-signing-keys.asc`)
 * in the `utils` module. The key file itself is the trust anchor -- since it is baked into
 * the JAR at build time, no additional fingerprint pinning is needed.
 *
 * To rotate the signing key: update `utils/src/main/resources/trusted-signing-keys.asc`
 * (append the new key, remove the old one) and release a new version.
 */
internal object GpgVerifier {

    private const val KEY_RESOURCE = "trusted-signing-keys.asc"

    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /** Loads the embedded trusted public key from classpath resources. */
    private fun loadTrustedKeys(): PGPPublicKeyRingCollection {
        val keyBytes = GpgVerifier::class.java.classLoader
            .getResourceAsStream(KEY_RESOURCE)
            ?.readBytes()
            ?: error("Embedded signing key not found: $KEY_RESOURCE")
        return readPublicKeys(keyBytes)
    }

    /**
     * Verifies that [signature] is a valid detached GPG signature for [data],
     * signed by one of the trusted embedded keys.
     *
     * @return `true` if the signature is valid and the signing key is trusted.
     * @throws IllegalStateException if the embedded key resource is missing.
     */
    fun verify(data: ByteArray, signature: ByteArray): Boolean {
        val pgpSignatures = readSignatures(signature)
        val keyRings = loadTrustedKeys()

        val sig = pgpSignatures.firstOrNull()
            ?: return false

        val verifyKey = keyRings.getPublicKey(sig.keyID)
            ?: return false // signing key not in our trusted set

        sig.init(JcaPGPContentVerifierBuilderProvider().setProvider("BC"), verifyKey)
        sig.update(data)

        return sig.verify()
    }

    private fun readSignatures(armoredSig: ByteArray): List<org.bouncycastle.openpgp.PGPSignature> {
        val decoded = PGPUtil.getDecoderStream(ByteArrayInputStream(armoredSig))
        val factory = JcaPGPObjectFactory(decoded)

        val result = mutableListOf<org.bouncycastle.openpgp.PGPSignature>()
        var obj = factory.nextObject()
        while (obj != null) {
            if (obj is PGPSignatureList) {
                for (i in 0 until obj.size()) {
                    result.add(obj[i])
                }
            }
            obj = factory.nextObject()
        }
        return result
    }

    private fun readPublicKeys(armoredKey: ByteArray): PGPPublicKeyRingCollection {
        val decoded = PGPUtil.getDecoderStream(ByteArrayInputStream(armoredKey))
        return PGPPublicKeyRingCollection(decoded, JcaKeyFingerprintCalculator())
    }
}
