package com.financetracker.evolva.data.model

/**
 * In-app languages. [tag] is a BCP-47 language tag used with
 * AppCompatDelegate.setApplicationLocales.
 */
enum class AppLanguage(
    val tag: String,
    /** Native / endonym label shown in the picker. */
    val nativeLabel: String,
    val englishLabel: String,
    /** Suggested home currency for a profile when this language is selected. */
    val suggestedCurrency: AppCurrency
) {
    ENGLISH("en", "English", "English", AppCurrency.USD),
    MALAY("ms", "Bahasa Melayu", "Malay", AppCurrency.MYR),
    MYANMAR("my", "မြန်မာ", "Myanmar", AppCurrency.MMK),
    INDONESIAN("id", "Bahasa Indonesia", "Indonesian", AppCurrency.IDR),
    TAMIL("ta", "தமிழ்", "Tamil", AppCurrency.INR),
    CHINESE("zh-CN", "中文", "Chinese", AppCurrency.CNY),
    JAPANESE("ja", "日本語", "Japanese", AppCurrency.JPY),
    KOREAN("ko", "한국어", "Korean", AppCurrency.KRW),
    RUSSIAN("ru", "Русский", "Russian", AppCurrency.RUB),
    THAI("th", "ไทย", "Thai", AppCurrency.THB),
    SPANISH("es", "Español", "Spanish", AppCurrency.EUR),
    FRENCH("fr", "Français", "French", AppCurrency.EUR),
    ITALIAN("it", "Italiano", "Italian", AppCurrency.EUR),
    VIETNAMESE("vi", "Tiếng Việt", "Vietnamese", AppCurrency.VND),
    TURKISH("tr", "Türkçe", "Turkish", AppCurrency.TRY),
    PERSIAN("fa", "فارسی", "Persian", AppCurrency.USD),
    GERMAN("de", "Deutsch", "German", AppCurrency.EUR),
    ARABIC("ar", "العربية", "Arabic", AppCurrency.SAR),
    URDU("ur", "اردو", "Urdu", AppCurrency.PKR),
    HINDI("hi", "हिन्दी", "Hindi", AppCurrency.INR),
    BENGALI("bn", "বাংলা", "Bengali", AppCurrency.BDT),
    ZULU("zu", "isiZulu", "Zulu", AppCurrency.ZAR),
    SHAN("shn", "လိၵ်ႈတႆး", "Shan", AppCurrency.MMK),
    DUTCH("nl", "Nederlands", "Dutch", AppCurrency.EUR);

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            if (tag.isNullOrBlank()) return ENGLISH
            val normalized = tag.replace('_', '-')
            return entries.find { it.tag.equals(normalized, ignoreCase = true) }
                ?: entries.find { normalized.startsWith(it.tag.substringBefore('-'), ignoreCase = true) }
                ?: ENGLISH
        }
    }
}
