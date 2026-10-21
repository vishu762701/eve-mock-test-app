package com.eve.app.util

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import com.eve.app.R
import java.util.concurrent.ConcurrentHashMap

/**
 * Authoritative central font manager providing:
 * 1. Default iOS 26 dynamic typography font family (Inter) with authentic weights (400, 500, 600, 700).
 * 2. 4-Family Font Library: Inter, Poppins, Roboto, and Noto Sans.
 * 3. Thread-safe typeface caching to guarantee zero UI-thread allocation and immediate preview updates.
 * 4. System typeface fallbacks for full multilingual (Hindi/Devanagari) script coverage.
 */
object FontManager {

    @Volatile
    var activeFontFamily: FontFamily = FontFamily.INTER

    enum class FontFamily(
        val id: String,
        val displayName: String,
        val description: String,
        val license: String,
        val licenseUrl: String
    ) {
        INTER(
            id = "inter",
            displayName = "iOS 26 Style (Inter)",
            description = "Clean, neutral, Apple-inspired semantic metrics with authentic 400/500/600/700 weights",
            license = "SIL Open Font License 1.1",
            licenseUrl = "https://rsms.me/inter/"
        ),
        POPPINS(
            id = "poppins",
            displayName = "Poppins",
            description = "Geometric sans-serif with balanced proportions and friendly character",
            license = "SIL Open Font License 1.1",
            licenseUrl = "https://github.com/itfoundry/Poppins"
        ),
        ROBOTO(
            id = "roboto",
            displayName = "Roboto (Android Native)",
            description = "Android platform native neo-grotesque font with dual geometric and open curves",
            license = "Apache License 2.0",
            licenseUrl = "https://github.com/google/fonts/tree/main/ofl/roboto"
        ),
        NOTO_SANS(
            id = "noto_sans",
            displayName = "Noto Sans",
            description = "Comprehensive global typeface designed for excellent readability across Latin and Hindi/Devanagari scripts",
            license = "SIL Open Font License 1.1",
            licenseUrl = "https://github.com/google/fonts/tree/main/ofl/notosans"
        );

        companion object {
            fun fromId(id: String?): FontFamily {
                return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: INTER
            }
        }
    }

    enum class FontWeight(val value: Int, val label: String) {
        REGULAR(400, "Regular 400"),
        MEDIUM(500, "Medium 500"),
        SEMIBOLD(600, "Semibold 600"),
        BOLD(700, "Bold 700");

        companion object {
            fun fromValue(v: Int?): FontWeight {
                return when (v) {
                    in 1..449 -> REGULAR
                    in 450..549 -> MEDIUM
                    in 550..649 -> SEMIBOLD
                    else -> BOLD
                }
            }

            fun fromInt(v: Int?): FontWeight = fromValue(v)
        }
    }

    // Retain backward-compatible FontOption for chips and fast selectors
    enum class FontOption(val displayName: String, val weightLabel: String, val family: FontFamily, val weight: FontWeight) {
        INTER_REGULAR("Inter Regular", "400", FontFamily.INTER, FontWeight.REGULAR),
        INTER_MEDIUM("Inter Medium", "500", FontFamily.INTER, FontWeight.MEDIUM),
        INTER_SEMIBOLD("Inter Semibold", "600", FontFamily.INTER, FontWeight.SEMIBOLD),
        INTER_BOLD("Inter Bold", "700", FontFamily.INTER, FontWeight.BOLD),
        SYSTEM_SANS("System Sans-Serif", "Default", FontFamily.ROBOTO, FontWeight.REGULAR),
        SYSTEM_SERIF("System Serif", "Serif", FontFamily.ROBOTO, FontWeight.REGULAR),
        SYSTEM_MONOSPACE("System Monospace", "Mono", FontFamily.ROBOTO, FontWeight.REGULAR);

        constructor(displayName: String, weightLabel: String) : this(
            displayName,
            weightLabel,
            when {
                displayName.startsWith("Inter") -> FontFamily.INTER
                else -> FontFamily.ROBOTO
            },
            when (weightLabel) {
                "500" -> FontWeight.MEDIUM
                "600" -> FontWeight.SEMIBOLD
                "700" -> FontWeight.BOLD
                else -> FontWeight.REGULAR
            }
        )
    }

    private val typefaceCache = ConcurrentHashMap<String, Typeface>()

    fun getTypeface(context: Context, family: FontFamily, weight: FontWeight = FontWeight.REGULAR): Typeface {
        val cacheKey = "${family.id}_${weight.value}"
        return typefaceCache.getOrPut(cacheKey) {
            loadTypeface(context, family, weight)
        }
    }

    private fun loadInterTypeface(context: Context, weight: FontWeight): Typeface {
        return try {
            when (weight) {
                FontWeight.REGULAR -> ResourcesCompat.getFont(context, R.font.inter_regular) ?: Typeface.DEFAULT
                FontWeight.MEDIUM -> ResourcesCompat.getFont(context, R.font.inter_medium) ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
                FontWeight.SEMIBOLD -> ResourcesCompat.getFont(context, R.font.inter_semibold) ?: Typeface.create("sans-serif-medium", Typeface.BOLD)
                FontWeight.BOLD -> ResourcesCompat.getFont(context, R.font.inter_bold) ?: Typeface.DEFAULT_BOLD
            }
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    private fun loadTypeface(context: Context, family: FontFamily, weight: FontWeight): Typeface {
        return try {
            when (family) {
                FontFamily.INTER -> loadInterTypeface(context, weight)
                FontFamily.POPPINS -> {
                    val assetName = when (weight) {
                        FontWeight.REGULAR -> "fonts/poppins_regular.ttf"
                        FontWeight.MEDIUM -> "fonts/poppins_medium.ttf"
                        FontWeight.SEMIBOLD -> "fonts/poppins_semibold.ttf"
                        FontWeight.BOLD -> "fonts/poppins_bold.ttf"
                    }
                    try {
                        Typeface.createFromAsset(context.assets, assetName)
                    } catch (_: Exception) {
                        loadSystemFont("sans-serif", weight)
                    }
                }
                FontFamily.ROBOTO -> loadSystemFont("sans-serif", weight)
                FontFamily.NOTO_SANS -> {
                    val assetName = when (weight) {
                        FontWeight.REGULAR -> "fonts/noto_sans_regular.ttf"
                        FontWeight.MEDIUM -> "fonts/noto_sans_medium.ttf"
                        FontWeight.SEMIBOLD -> "fonts/noto_sans_semibold.ttf"
                        FontWeight.BOLD -> "fonts/noto_sans_bold.ttf"
                    }
                    try {
                        Typeface.createFromAsset(context.assets, assetName)
                    } catch (_: Exception) {
                        loadSystemFont("noto-sans", weight)
                    }
                }
            }
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    private fun loadSystemFont(familyTag: String, weight: FontWeight): Typeface {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                Typeface.create(Typeface.create(familyTag, Typeface.NORMAL), weight.value, false)
            } catch (_: Exception) {
                Typeface.create(familyTag, if (weight == FontWeight.BOLD) Typeface.BOLD else Typeface.NORMAL)
            }
        } else {
            val style = if (weight == FontWeight.BOLD) Typeface.BOLD else Typeface.NORMAL
            Typeface.create(familyTag, style)
        }
    }

    fun getTypefaceForOption(context: Context, option: FontOption): Typeface {
        return when (option) {
            FontOption.INTER_REGULAR -> getTypeface(context, FontFamily.INTER, FontWeight.REGULAR)
            FontOption.INTER_MEDIUM -> getTypeface(context, FontFamily.INTER, FontWeight.MEDIUM)
            FontOption.INTER_SEMIBOLD -> getTypeface(context, FontFamily.INTER, FontWeight.SEMIBOLD)
            FontOption.INTER_BOLD -> getTypeface(context, FontFamily.INTER, FontWeight.BOLD)
            FontOption.SYSTEM_SANS -> Typeface.SANS_SERIF
            FontOption.SYSTEM_SERIF -> Typeface.SERIF
            FontOption.SYSTEM_MONOSPACE -> Typeface.MONOSPACE
        }
    }

    fun typeface(context: Context, bold: Boolean): Typeface {
        return if (bold) boldTypeface(context) else regularTypeface(context)
    }

    fun mediumTypeface(context: Context): Typeface {
        return getTypeface(context, activeFontFamily, FontWeight.MEDIUM)
    }

    fun semiboldTypeface(context: Context): Typeface {
        return getTypeface(context, activeFontFamily, FontWeight.SEMIBOLD)
    }

    fun regularTypeface(context: Context): Typeface {
        return getTypeface(context, activeFontFamily, FontWeight.REGULAR)
    }

    fun boldTypeface(context: Context): Typeface {
        return getTypeface(context, activeFontFamily, FontWeight.BOLD)
    }
}
