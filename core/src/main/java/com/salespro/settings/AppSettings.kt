package com.salespro.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.annotation.StringRes
import java.util.Locale

/** Languages the user can pick in More > Settings. */
enum class AppLanguage(val code: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    ARABIC("ar");

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

/**
 * App-wide user settings (language and currency), stored in SharedPreferences.
 *
 * Call [init] once from Application.onCreate. Activities should wrap their base
 * context with [wrap] so resources (and right-to-left layout for Arabic) follow
 * the chosen language. After changing a setting, recreate the activity.
 */
object AppSettings {
    private const val PREFS_NAME = "retailpilot_settings"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_CURRENCY = "currency"
    private const val KEY_LAST_BACKUP = "last_backup_at"

    /** Keeps the original behaviour for existing installs until the user picks a currency. */
    const val DEFAULT_CURRENCY = "INR"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        if (prefs != null) return
        val app: Context = context.applicationContext ?: context
        appContext = app
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val language: AppLanguage
        get() = AppLanguage.fromCode(prefs?.getString(KEY_LANGUAGE, null))

    fun setLanguage(language: AppLanguage) {
        prefs?.edit()?.putString(KEY_LANGUAGE, language.code)?.apply()
    }

    val currency: CurrencyOption
        get() = Currencies.find(prefs?.getString(KEY_CURRENCY, null))

    fun setCurrency(code: String) {
        prefs?.edit()?.putString(KEY_CURRENCY, code)?.apply()
    }

    /** Time of the last successful backup (0 = never). */
    val lastBackupAt: Long
        get() = prefs?.getLong(KEY_LAST_BACKUP, 0L) ?: 0L

    fun setLastBackupAt(time: Long) {
        prefs?.edit()?.putLong(KEY_LAST_BACKUP, time)?.apply()
    }

    /** Applies settings stored in a backup. Written synchronously: the app restarts right after. */
    fun restoreFromBackup(languageCode: String?, currencyCode: String?) {
        val editor = prefs?.edit() ?: return
        if (languageCode != null) editor.putString(KEY_LANGUAGE, languageCode)
        if (currencyCode != null) editor.putString(KEY_CURRENCY, currencyCode)
        editor.commit()
    }

    /** The locale chosen by the user, or null to follow the device language. */
    fun selectedLocale(): Locale? = when (val lang = language) {
        AppLanguage.SYSTEM -> null
        else -> Locale.forLanguageTag(lang.code)
    }

    /** Returns a context whose resources use the chosen language and layout direction. */
    fun wrap(base: Context): Context {
        init(base)
        val locale = selectedLocale() ?: return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    /** Localized string for code that has no Activity context (e.g. ViewModels). */
    fun string(@StringRes id: Int, vararg args: Any): String {
        val ctx = appContext ?: return ""
        val localized = wrap(ctx)
        return if (args.isEmpty()) localized.getString(id) else localized.getString(id, *args)
    }
}
