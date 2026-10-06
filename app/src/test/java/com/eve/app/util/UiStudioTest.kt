package com.eve.app.util

import com.eve.app.data.model.uistudio.*
import com.eve.app.data.repository.UiStudioRepository
import org.junit.Assert.*
import org.junit.Test

class UiStudioTest {

    private val repo = UiStudioRepository.getInstance()

    @Test
    fun testColorParsing() {
        // Standard 6-digit hex
        val white = UiStudioEngine.parseColorSafe("#FFFFFF")
        assertEquals(0xFFFFFFFF.toInt(), white)

        val black = UiStudioEngine.parseColorSafe("#000000")
        assertEquals(0xFF000000.toInt(), black)

        val navy = UiStudioEngine.parseColorSafe("#1E293B")
        assertEquals(0xFF1E293B.toInt(), navy)

        // 8-digit ARGB hex (translucent)
        val alphaTranslucent = UiStudioEngine.parseColorSafe("#801E293B")
        assertEquals(0x801E293B.toInt(), alphaTranslucent)

        // Without hash symbol
        val noHash = UiStudioEngine.parseColorSafe("1E293B")
        assertEquals(0xFF1E293B.toInt(), noHash)

        // Invalid / empty / malformed strings
        val fallback = 0x123456
        assertEquals(fallback, UiStudioEngine.parseColorSafe(null, fallback))
        assertEquals(fallback, UiStudioEngine.parseColorSafe("", fallback))
        assertEquals(fallback, UiStudioEngine.parseColorSafe("not-a-color", fallback))
        assertEquals(fallback, UiStudioEngine.parseColorSafe("#1234", fallback))
    }

    @Test
    fun testDefaultTemplateValidation() {
        val template = repo.getDefaultTemplate()
        assertNotNull(template)
        assertTrue(template.screens.containsKey("home"))
        assertTrue(template.screens.containsKey("test"))
        assertTrue(template.screens.containsKey("result"))

        // Check key components exist
        val homeComponents = template.screens["home"]?.components.orEmpty()
        assertTrue(homeComponents.containsKey("hero_banner"))
        assertTrue(homeComponents.containsKey("streak_pill"))
        assertTrue(homeComponents.containsKey("find_test_panel"))

        val testComponents = template.screens["test"]?.components.orEmpty()
        assertTrue(testComponents.containsKey("timer_pill"))
        assertTrue(testComponents.containsKey("question_card"))

        val resultComponents = template.screens["result"]?.components.orEmpty()
        assertTrue(resultComponents.containsKey("score_card"))

        // Validate template passes all schema rules
        val validation = repo.validateConfig(template)
        assertTrue("Default template should be 100% valid: ${validation.second}", validation.first)
        assertTrue(validation.second.isEmpty())
    }

    @Test
    fun testValidationRules() {
        val base = repo.getDefaultTemplate()

        // 1. Invalid hex color
        val invalidColorConfig = base.copy(
            screens = mapOf(
                "home" to ScreenConfig(
                    components = mapOf(
                        "hero_banner" to ComponentConfig(
                            appearance = AppearanceProperties(backgroundColor = "invalid-hex")
                        )
                    )
                )
            )
        )
        val res1 = repo.validateConfig(invalidColorConfig)
        assertFalse("Invalid color should fail validation", res1.first)
        assertTrue(res1.second.any { it.contains("backgroundColor", ignoreCase = true) })

        // 2. Negative dimension
        val negativeDimConfig = base.copy(
            screens = mapOf(
                "home" to ScreenConfig(
                    components = mapOf(
                        "hero_banner" to ComponentConfig(
                            layout = LayoutProperties(marginTop = -10)
                        )
                    )
                )
            )
        )
        val res2 = repo.validateConfig(negativeDimConfig)
        assertFalse("Negative margin should fail validation", res2.first)
        assertTrue(res2.second.any { it.contains("marginTop") })

        // 3. Out-of-bounds opacity
        val invalidOpacityConfig = base.copy(
            screens = mapOf(
                "home" to ScreenConfig(
                    components = mapOf(
                        "hero_banner" to ComponentConfig(
                            appearance = AppearanceProperties(opacity = 1.5f)
                        )
                    )
                )
            )
        )
        val res3 = repo.validateConfig(invalidOpacityConfig)
        assertFalse("Opacity > 1.0 should fail validation", res3.first)
        assertTrue(res3.second.any { it.contains("opacity") })

        // 4. Out-of-bounds corner radius
        val invalidRadiusConfig = base.copy(
            screens = mapOf(
                "home" to ScreenConfig(
                    components = mapOf(
                        "hero_banner" to ComponentConfig(
                            appearance = AppearanceProperties(cornerRadius = 200)
                        )
                    )
                )
            )
        )
        val res4 = repo.validateConfig(invalidRadiusConfig)
        assertFalse("Radius > 120 should fail validation", res4.first)
        assertTrue(res4.second.any { it.contains("cornerRadius") })
    }

    @Test
    fun testExportAndImportJson() {
        val customConfig = UiStudioConfig(
            version = 4,
            notes = "Custom Dark Slate Theme",
            screens = mapOf(
                "home" to ScreenConfig(
                    id = "home",
                    name = "Home Screen",
                    components = mapOf(
                        "hero_banner" to ComponentConfig(
                            id = "hero_banner",
                            visible = true,
                            layout = LayoutProperties(marginTop = 14, paddingStart = 20),
                            appearance = AppearanceProperties(backgroundColor = "#0F172A", cornerRadius = 24),
                            typography = TypographyProperties(textColor = "#FFFFFF", textSize = 20, textStyle = "bold")
                        )
                    )
                )
            )
        )

        // Export to JSON string
        val json = repo.exportToJson(customConfig)
        assertNotNull(json)
        assertTrue(json.contains("0F172A"))
        assertTrue(json.contains("Custom Dark Slate Theme"))

        // Re-import from JSON string
        val importResult = repo.importFromJson(json)
        assertTrue("Import should succeed", importResult.isSuccess)
        val imported = importResult.getOrThrow()

        assertEquals(4, imported.version)
        assertEquals("Custom Dark Slate Theme", imported.notes)
        val hero = imported.screens["home"]?.components?.get("hero_banner")
        assertNotNull(hero)
        assertEquals("#0F172A", hero?.appearance?.backgroundColor)
        assertEquals(24, hero?.appearance?.cornerRadius)
        assertEquals(14, hero?.layout?.marginTop)
        assertEquals(20, hero?.layout?.paddingStart)
        assertEquals("#FFFFFF", hero?.typography?.textColor)
        assertEquals(20, hero?.typography?.textSize)
    }

    @Test
    fun testEngineSafeViewNullability() {
        // Assert engine calls never throw on null views or null configs
        UiStudioEngine.applyToView(null, null)
        UiStudioEngine.applyTypography(null, null)
        UiStudioEngine.applyHeroBanner(null, null, null, null)
        UiStudioEngine.applyStreakPill(null, null, null)
    }
}
