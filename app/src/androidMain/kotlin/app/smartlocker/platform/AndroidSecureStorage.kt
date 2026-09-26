package app.smartlocker.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.smartlocker.shared.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidSecureStorage(context: Context) : SecureStorage {
    private val preferences = context.getSharedPreferences("smartlocker.secure", Context.MODE_PRIVATE)
    private val alias = "smartlocker.session.key.v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return generator.generateKey()
    }
    override suspend fun read(key: String): String? = withContext(Dispatchers.IO) {
        val raw = preferences.getString(key, null) ?: return@withContext null
        try {
            val bytes = Base64.decode(raw, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            cipher.updateAAD(key.toByteArray())
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)))
        } catch (_: Exception) {
            preferences.edit().remove(key).commit()
            throw AppFailure(FailureKind.EXPIRED_SESSION, "Sessão protegida indisponível. Entre novamente.")
        }
    }
    override suspend fun write(key: String, value: String?) = withContext(Dispatchers.IO) {
