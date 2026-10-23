package com.mahirex.nexoraplus.core.locale

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleUtil {
    /** The device (system) locale, independent of the per-app language. */
    fun systemLocale(): Locale {
        val list = ConfigurationCompat.getLocales(Resources.getSystem().configuration)
        return list[0] ?: Locale.ENGLISH
    }

    /** A context whose resources use [language]; used outside activities (widget, workers). */
    fun localizedContext(base: Context, language: AppLanguage): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language.tag))
        return base.createConfigurationContext(config)
    }

    /** Switches the per-app language; [LanguageCatalog.SYSTEM] follows the device again. Recreates the activity, no restart. */
    fun applyAppLocale(selected: String) {
        val list = if (selected == LanguageCatalog.SYSTEM) LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(selected)
        AppCompatDelegate.setApplicationLocales(list)
    }
}
