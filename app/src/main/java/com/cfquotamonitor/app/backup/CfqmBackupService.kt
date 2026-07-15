package com.cfquotamonitor.app.backup

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.time.Instant
import java.time.OffsetDateTime
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupAccount(
    val name: String,
    val accountId: String,
    val dailyLimit: Long,
    val token: String,
)

data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportedAtUtc: String = Instant.now().toString(),
    val sourcePlatform: String = "android",
    val accounts: List<BackupAccount>,
)

enum class DuplicateMode { SKIP, REPLACE, KEEP_BOTH }

data class ImportResult(val imported: Int, val skipped: Int)

enum class BackupError {
    SELECT_ACCOUNT,
    PASSWORD_SHORT,
    INVALID,
    UNSUPPORTED,
    WRONG_PASSWORD,
    TOKEN_UNREADABLE,
    READ_FAILED,
    WRITE_FAILED,
}

class BackupException(val reason: BackupError, cause: Throwable? = null) : Exception(reason.name, cause)

object CfqmBackupService {
    const val MAX_FILE_BYTES = 10 * 1024 * 1024
    private const val ITERATIONS = 310_000
    private const val KEY_BYTES = 32
    private const val TAG_BYTES = 16
    private const val MAX_ITERATIONS = 2_000_000
    private const val MIN_ITERATIONS = 100_000
    private val aad = "CFQM:1".toByteArray(Charsets.UTF_8)
    private val random = SecureRandom()

    fun export(accounts: List<BackupAccount>, password: String): ByteArray {
        if (accounts.isEmpty()) throw BackupException(BackupError.SELECT_ACCOUNT)
        if (password.length < 8) throw BackupException(BackupError.PASSWORD_SHORT)
        validateAccounts(accounts)

        val clear = payloadJson(BackupPayload(accounts = accounts)).toString().toByteArray(Charsets.UTF_8)
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val key = deriveKey(password, salt, ITERATIONS)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            cipher.updateAAD(aad)
            val combined = cipher.doFinal(clear)
            val ciphertext = combined.copyOfRange(0, combined.size - TAG_BYTES)
            val tag = combined.copyOfRange(combined.size - TAG_BYTES, combined.size)
            JSONObject()
                .put("format", "cfqm-encrypted-backup")
                .put("version", 1)
                .put("kdf", "PBKDF2-HMAC-SHA256")
                .put("iterations", ITERATIONS)
                .put("salt", encode(salt))
                .put("cipher", "AES-256-GCM")
                .put("nonce", encode(nonce))
                .put("tag", encode(tag))
                .put("payload", encode(ciphertext))
                .toString(2)
                .toByteArray(Charsets.UTF_8)
        } catch (error: BackupException) {
            throw error
        } catch (error: Exception) {
            throw BackupException(BackupError.WRITE_FAILED, error)
        } finally {
            clear.fill(0)
            key.fill(0)
        }
    }

    fun import(bytes: ByteArray, password: String): BackupPayload {
        if (bytes.isEmpty() || bytes.size > MAX_FILE_BYTES) throw BackupException(BackupError.INVALID)
        val container = try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (error: Exception) {
            throw BackupException(BackupError.INVALID, error)
        }

        val iterations = try {
            if (container.getString("format") != "cfqm-encrypted-backup" ||
                container.getInt("version") != 1 ||
                container.getString("kdf") != "PBKDF2-HMAC-SHA256" ||
                container.getString("cipher") != "AES-256-GCM"
            ) throw BackupException(BackupError.UNSUPPORTED)
            container.getInt("iterations")
        } catch (error: BackupException) {
            throw error
        } catch (error: Exception) {
            throw BackupException(BackupError.INVALID, error)
        }
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) throw BackupException(BackupError.UNSUPPORTED)

        val salt: ByteArray
        val nonce: ByteArray
        val tag: ByteArray
        val ciphertext: ByteArray
        try {
            salt = decode(container.getString("salt"))
            nonce = decode(container.getString("nonce"))
            tag = decode(container.getString("tag"))
            ciphertext = decode(container.getString("payload"))
        } catch (error: Exception) {
            throw BackupException(BackupError.INVALID, error)
        }
        if (salt.size != 16 || nonce.size != 12 || tag.size != TAG_BYTES ||
            ciphertext.isEmpty() || ciphertext.size > MAX_FILE_BYTES
        ) throw BackupException(BackupError.INVALID)

        val key = deriveKey(password, salt, iterations)
        val clear = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            cipher.updateAAD(aad)
            cipher.doFinal(ciphertext + tag)
        } catch (error: AEADBadTagException) {
            throw BackupException(BackupError.WRONG_PASSWORD, error)
        } catch (error: Exception) {
            throw BackupException(BackupError.INVALID, error)
        } finally {
            key.fill(0)
        }

        return try {
            parsePayload(JSONObject(String(clear, Charsets.UTF_8)))
        } catch (error: BackupException) {
            throw error
        } catch (error: Exception) {
            throw BackupException(BackupError.INVALID, error)
        } finally {
            clear.fill(0)
        }
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BYTES * 8)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } catch (error: Exception) {
            throw BackupException(BackupError.INVALID, error)
        } finally {
            spec.clearPassword()
        }
    }

    private fun payloadJson(payload: BackupPayload): JSONObject {
        val accounts = JSONArray()
        payload.accounts.forEach { account ->
            accounts.put(JSONObject()
                .put("name", account.name)
                .put("accountId", account.accountId)
                .put("dailyLimit", account.dailyLimit)
                .put("token", account.token))
        }
        return JSONObject()
            .put("schemaVersion", payload.schemaVersion)
            .put("exportedAtUtc", payload.exportedAtUtc)
            .put("sourcePlatform", payload.sourcePlatform)
            .put("accounts", accounts)
    }

    private fun parsePayload(json: JSONObject): BackupPayload {
        val schemaVersion = json.getInt("schemaVersion")
        if (schemaVersion != 1) throw BackupException(BackupError.INVALID)
        val exportedAt = json.getString("exportedAtUtc")
        val source = json.getString("sourcePlatform")
        try { OffsetDateTime.parse(exportedAt).toInstant() } catch (error: Exception) {
            throw BackupException(BackupError.INVALID, error)
        }
        if (source.isBlank() || source.length > 30) throw BackupException(BackupError.INVALID)
        val array = json.getJSONArray("accounts")
        if (array.length() !in 1..500) throw BackupException(BackupError.INVALID)
        val accounts = buildList {
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                add(BackupAccount(
                    name = item.getString("name").trim(),
                    accountId = item.getString("accountId").trim().lowercase(),
                    dailyLimit = item.getLong("dailyLimit"),
                    token = item.getString("token").trim(),
                ))
            }
        }
        validateAccounts(accounts)
        return BackupPayload(schemaVersion, exportedAt, source, accounts)
    }

    private fun validateAccounts(accounts: List<BackupAccount>) {
        if (accounts.size !in 1..500) throw BackupException(BackupError.INVALID)
        accounts.forEach { account ->
            if (account.name.length !in 1..100 ||
                !account.accountId.matches(Regex("^[a-f0-9]{32}${'$'}")) ||
                account.dailyLimit !in 1..10_000_000_000L ||
                account.token.length !in 20..4096
            ) throw BackupException(BackupError.INVALID)
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.DEFAULT)
}
