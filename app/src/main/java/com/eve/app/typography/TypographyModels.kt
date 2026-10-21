package com.eve.app.typography

import com.google.gson.annotations.SerializedName

/**
 * Versioned, typed schema for EVE Typography configuration.
 */
data class TypographyConfig(
    @SerializedName("schema_version") val schemaVersion: Int = 1,
    @SerializedName("profile_id") val profileId: String = "default",
    @SerializedName("preset_id") val presetId: String = "ios26_balanced",
    @SerializedName("font_family") val fontFamily: String = "inter",
    @SerializedName("global_styles") val globalStyles: Map<String, StyleRule> = emptyMap(),
    @SerializedName("screen_overrides") val screenOverrides: Map<String, ScreenTypographyConfig> = emptyMap(),
    @SerializedName("component_overrides") val componentOverrides: Map<String, StyleRule> = emptyMap(),
    @SerializedName("element_overrides") val elementOverrides: Map<String, StyleRule> = emptyMap(),
    @SerializedName("version") val version: Int = 1,
    @SerializedName("status") val status: String = "published",
    @SerializedName("published_at") val publishedAt: Long = 0L,
    @SerializedName("published_by") val publishedBy: String? = null,
    @SerializedName("updated_at") val updatedAt: Long = 0L,
    @SerializedName("updated_by") val updatedBy: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class StyleRule(
    @SerializedName("role") val role: String? = null,
    @SerializedName("font_family") val fontFamily: String? = null,
    @SerializedName("text_size_sp") val textSizeSp: Float? = null,
    @SerializedName("weight") val weight: Int? = null,
    @SerializedName("line_height_sp") val lineHeightSp: Float? = null,
    @SerializedName("letter_spacing") val letterSpacing: Float? = null,
    @SerializedName("text_align") val textAlign: String? = null
)

data class ScreenTypographyConfig(
    @SerializedName("screen_key") val screenKey: String = "",
    @SerializedName("role_overrides") val roleOverrides: Map<String, StyleRule> = emptyMap(),
    @SerializedName("element_overrides") val elementOverrides: Map<String, StyleRule> = emptyMap()
)

data class TypographyPreset(
    val id: String,
    val name: String,
    val description: String,
    val defaultFontFamily: String,
    val rules: Map<String, StyleRule>
) {
    companion object {
        val IOS26_BALANCED = TypographyPreset(
            id = "ios26_balanced",
            name = "iOS 26 Balanced",
            description = "Apple iOS 26 HIG scale with restrained Semibold 600 buttons & headlines, Regular 400 body",
            defaultFontFamily = "inter",
            rules = mapOf(
                "large_title" to StyleRule(textSizeSp = 34f, lineHeightSp = 41f, weight = 400),
                "title_1" to StyleRule(textSizeSp = 28f, lineHeightSp = 34f, weight = 700),
                "title_2" to StyleRule(textSizeSp = 22f, lineHeightSp = 28f, weight = 700),
                "title_3" to StyleRule(textSizeSp = 20f, lineHeightSp = 25f, weight = 600),
                "headline" to StyleRule(textSizeSp = 17f, lineHeightSp = 22f, weight = 600),
                "body" to StyleRule(textSizeSp = 17f, lineHeightSp = 22f, weight = 400),
                "callout" to StyleRule(textSizeSp = 16f, lineHeightSp = 21f, weight = 400),
                "subheadline" to StyleRule(textSizeSp = 15f, lineHeightSp = 20f, weight = 400),
                "footnote" to StyleRule(textSizeSp = 13f, lineHeightSp = 18f, weight = 400),
                "caption_1" to StyleRule(textSizeSp = 12f, lineHeightSp = 16f, weight = 400),
                "caption_2" to StyleRule(textSizeSp = 11f, lineHeightSp = 13f, weight = 500)
            )
        )

        val IOS26_HIG_REFERENCE = TypographyPreset(
            id = "ios26_hig_reference",
            name = "iOS 26 HIG Reference",
            description = "Strict Apple Large Category reference scale with Bold 700 titles",
            defaultFontFamily = "inter",
            rules = mapOf(
                "large_title" to StyleRule(textSizeSp = 34f, lineHeightSp = 41f, weight = 700),
                "title_1" to StyleRule(textSizeSp = 28f, lineHeightSp = 34f, weight = 700),
                "title_2" to StyleRule(textSizeSp = 22f, lineHeightSp = 28f, weight = 700),
                "title_3" to StyleRule(textSizeSp = 20f, lineHeightSp = 25f, weight = 600),
                "headline" to StyleRule(textSizeSp = 17f, lineHeightSp = 22f, weight = 600),
                "body" to StyleRule(textSizeSp = 17f, lineHeightSp = 22f, weight = 400),
                "callout" to StyleRule(textSizeSp = 16f, lineHeightSp = 21f, weight = 400),
                "subheadline" to StyleRule(textSizeSp = 15f, lineHeightSp = 20f, weight = 400),
                "footnote" to StyleRule(textSizeSp = 13f, lineHeightSp = 18f, weight = 400),
                "caption_1" to StyleRule(textSizeSp = 12f, lineHeightSp = 16f, weight = 400),
                "caption_2" to StyleRule(textSizeSp = 11f, lineHeightSp = 13f, weight = 500)
            )
        )

        val POPPINS_CLEAN = TypographyPreset(
            id = "poppins_clean",
            name = "Poppins Clean",
            description = "Friendly geometric styling with authentic Poppins weights",
            defaultFontFamily = "poppins",
            rules = mapOf(
                "large_title" to StyleRule(textSizeSp = 32f, lineHeightSp = 40f, weight = 600),
                "title_1" to StyleRule(textSizeSp = 26f, lineHeightSp = 32f, weight = 600),
                "title_2" to StyleRule(textSizeSp = 21f, lineHeightSp = 27f, weight = 600),
                "title_3" to StyleRule(textSizeSp = 19f, lineHeightSp = 24f, weight = 500),
                "headline" to StyleRule(textSizeSp = 16f, lineHeightSp = 22f, weight = 500),
                "body" to StyleRule(textSizeSp = 15f, lineHeightSp = 21f, weight = 400),
                "callout" to StyleRule(textSizeSp = 14f, lineHeightSp = 20f, weight = 400),
                "subheadline" to StyleRule(textSizeSp = 14f, lineHeightSp = 19f, weight = 400),
                "footnote" to StyleRule(textSizeSp = 12f, lineHeightSp = 17f, weight = 400),
                "caption_1" to StyleRule(textSizeSp = 11f, lineHeightSp = 15f, weight = 400),
                "caption_2" to StyleRule(textSizeSp = 10f, lineHeightSp = 13f, weight = 500)
            )
        )

        val ROBOTO_NEUTRAL = TypographyPreset(
            id = "roboto_neutral",
            name = "Roboto Neutral",
            description = "Android native neo-grotesque styling with standard Material proportions",
            defaultFontFamily = "roboto",
            rules = mapOf(
                "large_title" to StyleRule(textSizeSp = 34f, lineHeightSp = 40f, weight = 400),
                "title_1" to StyleRule(textSizeSp = 28f, lineHeightSp = 34f, weight = 500),
                "title_2" to StyleRule(textSizeSp = 22f, lineHeightSp = 28f, weight = 500),
                "title_3" to StyleRule(textSizeSp = 20f, lineHeightSp = 26f, weight = 500),
                "headline" to StyleRule(textSizeSp = 16f, lineHeightSp = 24f, weight = 500),
                "body" to StyleRule(textSizeSp = 16f, lineHeightSp = 24f, weight = 400),
                "callout" to StyleRule(textSizeSp = 14f, lineHeightSp = 20f, weight = 400),
                "subheadline" to StyleRule(textSizeSp = 14f, lineHeightSp = 20f, weight = 400),
                "footnote" to StyleRule(textSizeSp = 12f, lineHeightSp = 16f, weight = 400),
                "caption_1" to StyleRule(textSizeSp = 12f, lineHeightSp = 16f, weight = 400),
                "caption_2" to StyleRule(textSizeSp = 11f, lineHeightSp = 14f, weight = 500)
            )
        )

        val NOTO_READABLE = TypographyPreset(
            id = "noto_readable",
            name = "Noto Readable",
            description = "Multilingual-first typography scale with optimized Hindi/Devanagari metrics",
            defaultFontFamily = "noto_sans",
            rules = mapOf(
                "large_title" to StyleRule(textSizeSp = 34f, lineHeightSp = 42f, weight = 600),
                "title_1" to StyleRule(textSizeSp = 28f, lineHeightSp = 35f, weight = 600),
                "title_2" to StyleRule(textSizeSp = 22f, lineHeightSp = 29f, weight = 600),
                "title_3" to StyleRule(textSizeSp = 20f, lineHeightSp = 26f, weight = 500),
                "headline" to StyleRule(textSizeSp = 17f, lineHeightSp = 23f, weight = 500),
                "body" to StyleRule(textSizeSp = 16f, lineHeightSp = 23f, weight = 400),
                "callout" to StyleRule(textSizeSp = 15f, lineHeightSp = 22f, weight = 400),
                "subheadline" to StyleRule(textSizeSp = 14f, lineHeightSp = 21f, weight = 400),
                "footnote" to StyleRule(textSizeSp = 13f, lineHeightSp = 19f, weight = 400),
                "caption_1" to StyleRule(textSizeSp = 12f, lineHeightSp = 17f, weight = 400),
                "caption_2" to StyleRule(textSizeSp = 11f, lineHeightSp = 15f, weight = 500)
            )
        )

        val ALL = listOf(IOS26_BALANCED, IOS26_HIG_REFERENCE, POPPINS_CLEAN, ROBOTO_NEUTRAL, NOTO_READABLE)

        fun find(id: String?): TypographyPreset {
            return ALL.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: IOS26_BALANCED
        }
    }
}
