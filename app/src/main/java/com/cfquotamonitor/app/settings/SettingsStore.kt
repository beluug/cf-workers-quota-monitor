package com.cfquotamonitor.app.settings

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import com.cfquotamonitor.app.R
import com.cfquotamonitor.app.background.BackgroundRefreshScheduler
import java.util.Locale

data class AppSettings(
    val lockEnabled: Boolean = false,
    val languageTag: String = "",
    val backgroundRefreshEnabled: Boolean = false,
    val refreshIntervalMinutes: Long = 60L,
)

data class AppLanguage(
    val tag: String,
    @StringRes val labelRes: Int,
)

val supportedLanguages = listOf(
    AppLanguage("", R.string.language_system),
    AppLanguage("zh", R.string.language_chinese),
    AppLanguage("en", R.string.language_english),
    AppLanguage("ru", R.string.language_russian),
    AppLanguage("it", R.string.language_italian),
    AppLanguage("fr", R.string.language_french),
    AppLanguage("es", R.string.language_spanish),
    AppLanguage("ar", R.string.language_arabic),
)

class SettingsStore(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        lockEnabled = prefs.getBoolean(KEY_LOCK, false),
        languageTag = prefs.getString(KEY_LANGUAGE, "").orEmpty(),
        backgroundRefreshEnabled = prefs.getBoolean(KEY_BACKGROUND, false),
        refreshIntervalMinutes = prefs.getLong(KEY_INTERVAL, 60L).coerceAtLeast(15L),
    )

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK, enabled).apply()
    }

    fun setLanguage(tag: String) {
        prefs.edit().putString(KEY_LANGUAGE, tag).apply()
    }

    fun setBackgroundRefresh(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND, enabled).apply()
        val settings = load()
        BackgroundRefreshScheduler.apply(context, settings)
    }

    fun setRefreshInterval(minutes: Long) {
        prefs.edit().putLong(KEY_INTERVAL, minutes.coerceAtLeast(15L)).apply()
        val settings = load()
        BackgroundRefreshScheduler.apply(context, settings)
    }

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_LOCK = "lock_enabled"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_BACKGROUND = "background_refresh"
        private const val KEY_INTERVAL = "refresh_interval_minutes"

        fun localizedContext(base: Context): Context {
            val tag = base.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, "")
                .orEmpty()
            val locale = if (tag.isBlank()) {
                val systemLocale = base.resources.configuration.locales[0]
                // Treat every Chinese regional variant as the supported Chinese UI.
                if (systemLocale.language == Locale.CHINESE.language) {
                    Locale.forLanguageTag("zh")
                } else {
                    return base
                }
            } else {
                Locale.forLanguageTag(tag)
            }
            Locale.setDefault(locale)
            val configuration = Configuration(base.resources.configuration).apply {
                setLocales(LocaleList(locale))
                setLayoutDirection(locale)
            }
            return base.createConfigurationContext(configuration)
        }
    }
}
