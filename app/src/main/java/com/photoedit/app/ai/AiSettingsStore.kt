package com.photoedit.app.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.photoedit.app.domain.AiRules
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Local credentials only; callers perform disk/Keystore work on Dispatchers.IO. */
class AiSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("cloudflare_private", Context.MODE_PRIVATE)
    private val alias = "PhotoEditCloudflareToken"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun load(): Pair<String, String> {
        val account = prefs.getString("account", AiRules.DEFAULT_ACCOUNT) ?: AiRules.DEFAULT_ACCOUNT
        val stored = prefs.getString("token", null) ?: return account to ""
        val parts = stored.split(":")
        require(parts.size == 2) { "本机密钥数据损坏，请清除设置后重新填写" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        return account to cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }
    fun save(account: String, token: String) {
        AiRules.validateCredentials(account, token)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val value = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(prefs.edit().putString("account", account).putString("token", value).commit()) { "设置保存失败" }
    }
    fun clear() { check(prefs.edit().clear().commit()) { "清除设置失败" } }
}
