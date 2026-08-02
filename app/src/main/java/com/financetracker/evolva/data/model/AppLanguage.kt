package com.financetracker.evolva.data.model

/**
 * In-app languages. [tag] is a BCP-47 language tag used with
 * AppCompatDelegate.setApplicationLocales.
 */
enum class AppLanguage(
    val tag: String,
    /** Native / endonym label shown in the picker. */
    val nativeLabel: String,
    val englishLabel: String
) {
    ENGLISH("en", "English", "English"),
    MALAY("ms", "Bahasa Melayu", "Malay"),
    MYANMAR("my", "မြန်မာ", "Myanmar"),
    INDONESIAN("id", "Bahasa Indonesia", "Indonesian"),
    TAMIL("ta", "தமிழ்", "Tamil"),
    CHINESE("zh-CN", "中文", "Chinese"),
    JAPANESE("ja", "日本語", "Japanese"),
    KOREAN("ko", "한국어", "Korean"),
    RUSSIAN("ru", "Русский", "Russian"),
    THAI("th", "ไทย", "Thai"),
    SPANISH("es", "Español", "Spanish"),
    FRENCH("fr", "Français", "French"),
    ITALIAN("it", "Italiano", "Italian"),
    VIETNAMESE("vi", "Tiếng Việt", "Vietnamese"),
    TURKISH("tr", "Türkçe", "Turkish"),
    PERSIAN("fa", "فارسی", "Persian"),
    GERMAN("de", "Deutsch", "German"),
    ARABIC("ar", "العربية", "Arabic"),
    URDU("ur", "اردو", "Urdu"),
    HINDI("hi", "हिन्दी", "Hindi");

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
