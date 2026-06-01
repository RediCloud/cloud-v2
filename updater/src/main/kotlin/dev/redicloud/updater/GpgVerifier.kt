package dev.redicloud.updater

import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSignatureList
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider
import java.io.ByteArrayInputStream

/**
 * Verifies detached GPG (ASCII-armored) signatures using Bouncy Castle.
 */
internal object GpgVerifier {

    /**
     * Verifies that [signature] is a valid GPG detached signature for [data],
     * signed by a key in [publicKey].
     *
     * All inputs are raw bytes (ASCII-armored `.asc` files are decoded internally).
     *
     * @return `true` if the signature is valid.
     */
    fun verify(data: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        val pgpSignatures = readSignatures(signature)
        val keyRings = readPublicKeys(publicKey)

        val sig = pgpSignatures.firstOrNull()
            ?: return false

        val verifyKey = keyRings.getPublicKey(sig.keyID)
            ?: return false

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
