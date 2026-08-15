package net.hanenashi.tilezz

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList

object LocaleController {
    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_JAPANESE = "ja"
    const val LANGUAGE_CZECH = "cs"

    fun currentLanguage(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return LANGUAGE_SYSTEM
        }
        val tags = context.getSystemService(LocaleManager::class.java)
            .applicationLocales
            .toLanguageTags()
        return when (tags) {
            LANGUAGE_ENGLISH -> LANGUAGE_ENGLISH
            LANGUAGE_JAPANESE -> LANGUAGE_JAPANESE
            LANGUAGE_CZECH -> LANGUAGE_CZECH
            else -> LANGUAGE_SYSTEM
        }
    }

    fun setLanguage(context: Context, language: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val tags = when (language) {
            LANGUAGE_ENGLISH -> LANGUAGE_ENGLISH
            LANGUAGE_JAPANESE -> LANGUAGE_JAPANESE
            LANGUAGE_CZECH -> LANGUAGE_CZECH
            else -> ""
        }
        context.getSystemService(LocaleManager::class.java)
            .applicationLocales = LocaleList.forLanguageTags(tags)
    }

    fun localizedContext(context: Context): Context {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return context
        }
        val locales = context.getSystemService(LocaleManager::class.java)
            .applicationLocales
        if (locales.isEmpty) {
            return context
        }
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(locales)
        return context.createConfigurationContext(configuration)
    }
}
