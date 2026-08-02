package com.financetracker.evolva.data.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.financetracker.evolva.data.model.AppLanguage

object LocaleHelper {
    fun apply(language: AppLanguage) {
        val locales = LocaleListCompat.forLanguageTags(language.tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun current(): AppLanguage {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return AppLanguage.fromTag(tags.ifBlank { null })
    }
}
