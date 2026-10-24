package com.eve.app.typography

import com.eve.app.util.FontManager

/**
 * Resolves effective typography according to the authoritative 6-stage resolution order:
 * 1. Element-specific override
 * 2. Component-specific override
 * 3. Screen-specific role override
 * 4. Global semantic role
 * 5. Active preset
 * 6. Bundled safe default
 */
object TypographyResolver {
    const val MIN_TEXT_SIZE_SP = 8f
    const val MAX_TEXT_SIZE_SP = 60f

    data class ResolvedStyle(
        val fontFamily: FontManager.FontFamily,
        val weight: FontManager.FontWeight,
        val textSizeSp: Float,
        val lineHeightSp: Float,
        val letterSpacing: Float,
        val source: String
    )

    data class ValidationIssue(
        val level: Level,
        val message: String
    ) {
        enum class Level { WARNING, ERROR }
    }

    /**
     * Resolves the effective typography style for a given screen and element.
     */
    fun resolve(
        config: TypographyConfig,
        screenKey: String?,
        elementKey: String?,
        componentRole: String? = null
    ): ResolvedStyle {
        val elementDesc = elementKey?.let { TypographyRegistry.findElement(it) }
        val role = componentRole ?: elementDesc?.defaultRole ?: "body"
        val activePreset = TypographyPreset.find(config.presetId)
        val defaultPreset = TypographyPreset.TELEGRAM_NATIVE

        // 1. Element-specific override
        val elementOverride = if (elementKey != null && screenKey != null) {
            config.screenOverrides[screenKey]?.elementOverrides?.get(elementKey)
                ?: config.elementOverrides[elementKey]
        } else if (elementKey != null) {
            config.elementOverrides[elementKey]
        } else null

        if (elementOverride != null && hasAnyRule(elementOverride)) {
            return buildResolvedStyle(
                rule = elementOverride,
                fallbackFamily = config.fontFamily,
                fallbackRule = activePreset.rules[role] ?: defaultPreset.rules[role],
                source = "Element override ($elementKey)"
            )
        }

        // 2. Component-specific override
        val compOverride = componentRole?.let { config.componentOverrides[it] }
        if (compOverride != null && hasAnyRule(compOverride)) {
            return buildResolvedStyle(
                rule = compOverride,
                fallbackFamily = config.fontFamily,
                fallbackRule = activePreset.rules[role] ?: defaultPreset.rules[role],
                source = "Component override ($componentRole)"
            )
        }

        // 3. Screen-specific role override
        val screenRoleOverride = screenKey?.let { config.screenOverrides[it]?.roleOverrides?.get(role) }
        if (screenRoleOverride != null && hasAnyRule(screenRoleOverride)) {
            return buildResolvedStyle(
                rule = screenRoleOverride,
                fallbackFamily = config.fontFamily,
                fallbackRule = activePreset.rules[role] ?: defaultPreset.rules[role],
                source = "Screen override ($screenKey : $role)"
            )
        }

        // 4. Global semantic role
        val globalRole = config.globalStyles[role]
        if (globalRole != null && hasAnyRule(globalRole)) {
            return buildResolvedStyle(
                rule = globalRole,
                fallbackFamily = config.fontFamily,
                fallbackRule = activePreset.rules[role] ?: defaultPreset.rules[role],
                source = "Global semantic role ($role)"
            )
        }

        // 5. Active preset
        val presetRule = activePreset.rules[role]
        if (presetRule != null) {
            return buildResolvedStyle(
                rule = presetRule,
                fallbackFamily = activePreset.defaultFontFamily,
                fallbackRule = defaultPreset.rules[role],
                source = "Preset (${activePreset.name})"
            )
        }

        // 6. Bundled safe default
        val safeRule = defaultPreset.rules[role] ?: StyleRule(textSizeSp = 17f, lineHeightSp = 22f, weight = 400)
        return buildResolvedStyle(
            rule = safeRule,
            fallbackFamily = "roboto",
            fallbackRule = null,
            source = "Bundled safe default"
        )
    }

    private fun hasAnyRule(r: StyleRule): Boolean {
        return r.textSizeSp != null || r.weight != null || r.lineHeightSp != null ||
                r.letterSpacing != null || r.fontFamily != null
    }

    private fun buildResolvedStyle(
        rule: StyleRule,
        fallbackFamily: String,
        fallbackRule: StyleRule?,
        source: String
    ): ResolvedStyle {
        val familyStr = rule.fontFamily ?: fallbackFamily
        val family = FontManager.FontFamily.fromId(familyStr)
        val weightVal = rule.weight ?: fallbackRule?.weight ?: 400
        val weight = FontManager.FontWeight.fromValue(weightVal)

        val size = rule.textSizeSp ?: fallbackRule?.textSizeSp ?: 17f
        val line = rule.lineHeightSp ?: fallbackRule?.lineHeightSp ?: (size * 1.3f)
        val letter = rule.letterSpacing ?: fallbackRule?.letterSpacing ?: 0f

        return ResolvedStyle(
            fontFamily = family,
            weight = weight,
            textSizeSp = size.coerceIn(MIN_TEXT_SIZE_SP, MAX_TEXT_SIZE_SP),
            lineHeightSp = line.coerceAtLeast(size),
            letterSpacing = letter.coerceIn(-0.05f, 0.2f),
            source = source
        )
    }

    /**
     * Validates configuration for accessibility, clipping, and rendering safety.
     */
    fun validate(config: TypographyConfig): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        // Check global styles
        for ((role, rule) in config.globalStyles) {
            validateRule("Global role '$role'", rule, issues)
        }

        // Check screen overrides
        for ((screenKey, screenConfig) in config.screenOverrides) {
            for ((role, rule) in screenConfig.roleOverrides) {
                validateRule("Screen '$screenKey' role '$role'", rule, issues)
            }
            for ((elementKey, rule) in screenConfig.elementOverrides) {
                validateRule("Screen '$screenKey' element '$elementKey'", rule, issues)
            }
        }

        return issues
    }

    private fun validateRule(context: String, rule: StyleRule, issues: MutableList<ValidationIssue>) {
        rule.textSizeSp?.let { size ->
            if (size < 10f) {
                issues.add(ValidationIssue(ValidationIssue.Level.WARNING, "$context text size (${size}sp) may be unreadably small on small screens."))
            } else if (size > 48f) {
                issues.add(ValidationIssue(ValidationIssue.Level.WARNING, "$context text size (${size}sp) may cause clipping in compact containers."))
            }
            Unit
        }

        rule.lineHeightSp?.let { line ->
            val size = rule.textSizeSp ?: 16f
            if (line < size) {
                issues.add(ValidationIssue(ValidationIssue.Level.ERROR, "$context line height (${line}sp) is smaller than text size (${size}sp), causing vertical overlapping."))
            }
            Unit
        }

        rule.letterSpacing?.let { spacing ->
            if (spacing > 0.15f) {
                issues.add(ValidationIssue(ValidationIssue.Level.WARNING, "$context letter spacing ($spacing) is high and may decrease reading fluency."))
            }
            Unit
        }
    }
}
