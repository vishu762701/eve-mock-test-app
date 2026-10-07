package com.eve.app.util

import com.eve.app.data.model.uistudio.*
import com.eve.app.data.repository.UiStudioRepository
import com.eve.app.uistudio.UiStudioRegistry
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

        // Check that all registered real application screens exist in the template
        assertTrue(template.screens.containsKey("home"))
        assertTrue(template.screens.containsKey("test"))
        assertTrue(template.screens.containsKey("result"))
        assertTrue(template.screens.containsKey("profile"))
        assertTrue(template.screens.containsKey("notifications"))
        assertTrue(template.screens.containsKey("syllabus"))
        assertTrue(template.screens.containsKey("login"))
        assertTrue(template.screens.containsKey("admin"))

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

        // 5. Out-of-bounds blur radius
        val invalidBlurConfig = base.copy(
            screens = mapOf(
                "home" to ScreenConfig(
                    components = mapOf(
                        "hero_banner" to ComponentConfig(
                            material = MaterialProperties(blurRadius = 99)
                        )
                    )
                )
            )
        )
        val res5 = repo.validateConfig(invalidBlurConfig)
        assertFalse("Blur radius > 50 should fail validation", res5.first)
        assertTrue(res5.second.any { it.contains("blurRadius") })

        // 6. Valid Material and Animation config
        val validMaterialConfig = base.copy(
            screens = mapOf(
                "home" to ScreenConfig(
                    components = mapOf(
                        "hero_banner" to ComponentConfig(
                            material = MaterialProperties(blurRadius = 20, materialOpacity = 0.85f, tintColor = "#1E293B", tintOpacity = 0.7f),
                            animation = AnimationProperties(enabled = true, durationMs = 350L, delayMs = 50L)
                        )
                    )
                )
            )
        )
        val res6 = repo.validateConfig(validMaterialConfig)
        assertTrue("Valid material & animation should pass: ${res6.second}", res6.first)
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
                            material = MaterialProperties(blurRadius = 15, materialOpacity = 0.9f),
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
        assertEquals(15, hero?.material?.blurRadius)
        assertEquals(14, hero?.layout?.marginTop)
        assertEquals(20, hero?.layout?.paddingStart)
        assertEquals("#FFFFFF", hero?.typography?.textColor)
        assertEquals(20, hero?.typography?.textSize)
    }

    @Test
    fun testRegistryComponentsAndScreens() {
        assertTrue(UiStudioRegistry.SUPPORTED_SCREENS.isNotEmpty())
        assertTrue(UiStudioRegistry.COMPONENT_TYPES.isNotEmpty())

        val buttonComp = UiStudioRegistry.createDefaultComponent("btn_test", "button", "Test Button")
        assertEquals("button", buttonComp.type)
        assertEquals("btn_test", buttonComp.id)
        assertEquals("Test Button", buttonComp.name)
        assertNotNull(buttonComp.appearance.backgroundColor)
    }

    @Test
    fun testEngineSafeViewNullability() {
        // Assert engine calls never throw on null views or null configs
        UiStudioEngine.applyToView(null, null)
        UiStudioEngine.applyTypography(null, null)
        UiStudioEngine.applyHeroBanner(null, null, null, null)
        UiStudioEngine.applyStreakPill(null, null, null)
        UiStudioEngine.playEntranceAnimation(null, null)
    }

    @Test
    fun testScreenTransitionsAndAdvancedRawJson() {
        val json = """
            {
              "id": "banner_custom",
              "type": "banner",
              "name": "Custom Live Banner",
              "visible": true,
              "enabled": true,
              "order": 1,
              "isProtected": false,
              "layout": {
                "width": "match_parent",
                "height": "wrap_content",
                "marginTop": 12,
                "marginBottom": 8
              },
              "appearance": {
                "backgroundColor": "#000000",
                "cornerRadius": 16,
                "strokeColor": "#334155",
                "strokeWidth": 1
              },
              "material": {
                "blurRadius": 25,
                "materialOpacity": 0.8,
                "tintColor": "#007AFF",
                "tintOpacity": 0.5
              },
              "animation": {
                "enabled": true,
                "type": "fade_scale",
                "durationMs": 300,
                "delayMs": 50,
                "interpolator": "overshoot"
              },
              "actions": {
                "actionType": "open_screen",
                "actionTarget": "test"
              }
            }
        """.trimIndent()

        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
        val comp = gson.fromJson(json, ComponentConfig::class.java)

        assertEquals("banner_custom", comp.id)
        assertEquals("banner", comp.type)
        assertEquals("Custom Live Banner", comp.name)
        assertEquals("match_parent", comp.layout.width)
        assertEquals("wrap_content", comp.layout.height)
        assertEquals(25, comp.material.blurRadius)
        assertEquals(0.8f, comp.material.materialOpacity)
        assertEquals("fade_scale", comp.animation.type)
        assertEquals("open_screen", comp.actions.actionType)
        assertEquals("test", comp.actions.actionTarget)

        // Verify re-serialization
        val reserialized = gson.toJson(comp)
        assertTrue(reserialized.contains("banner_custom"))
        assertTrue(reserialized.contains("fade_scale"))
    }

    @Test
    fun testScreenConfigTransitions() {
        val screenWithTransition = ScreenConfig(
            id = "test",
            name = "Test Screen",
            transition = "fade_scale"
        )
        assertEquals("fade_scale", screenWithTransition.transition)

        val fullConfig = UiStudioConfig(
            screens = mapOf("test" to screenWithTransition)
        )
        val json = repo.exportToJson(fullConfig)
        val imported = repo.importFromJson(json).getOrThrow()
        assertEquals("fade_scale", imported.screens["test"]?.transition)
    }

    @Test
    fun testBrandingAndDesignSystemValidation() {
        val validConfig = UiStudioConfig(
            revision = "rev-101",
            branding = BrandingConfig(
                appDisplayName = "EVE Super App",
                brandColor = "#007AFF",
                globalBackgroundColor = "#000000"
            ),
            designSystem = DesignSystemConfig(
                appBackground = "#000000",
                surfaceBackground = "#1E293B",
                textPrimary = "#FFFFFF",
                accentColor = "#007AFF"
            ),
            screens = mapOf(
                "home" to ScreenConfig(
                    id = "home",
                    backgroundColor = "#000000"
                )
            )
        )
        val valResult = repo.validateConfig(validConfig)
        assertTrue("Valid branding and designSystem should pass validation: ${valResult.second}", valResult.first)

        // Invalid branding brandColor
        val invalidBrand = validConfig.copy(
            branding = validConfig.branding.copy(brandColor = "not-a-color")
        )
        val valResult2 = repo.validateConfig(invalidBrand)
        assertFalse("Invalid brandColor should fail", valResult2.first)
        assertTrue(valResult2.second.any { it.contains("brandColor") })

        // Invalid design system accentColor
        val invalidDs = validConfig.copy(
            designSystem = validConfig.designSystem.copy(accentColor = "#XYZ123")
        )
        val valResult3 = repo.validateConfig(invalidDs)
        assertFalse("Invalid accentColor should fail", valResult3.first)
        assertTrue(valResult3.second.any { it.contains("accentColor") })
    }

    @Test
    fun testStyleClipboardCopyAndPaste() {
        val sourceComp = ComponentConfig(
            id = "source_card",
            name = "Source Card",
            appearance = AppearanceProperties(
                backgroundColor = "#0F172A",
                cornerRadius = 22,
                strokeColor = "#38BDF8",
                strokeWidth = 2,
                elevation = 4,
                opacity = 0.9f
            ),
            material = MaterialProperties(
                blurRadius = 18,
                materialOpacity = 0.85f,
                tintColor = "#0F172A",
                tintOpacity = 0.6f
            )
        )

        repo.copyStyle(sourceComp)
        assertTrue(repo.hasCopiedStyle())

        val targetComp = ComponentConfig(
            id = "target_btn",
            name = "Target Button",
            appearance = AppearanceProperties(
                backgroundColor = "#FFFFFF",
                cornerRadius = 8
            )
        )

        val pasted = repo.pasteStyle(targetComp)
        // Verified: target ID and name are preserved, appearance and material are copied!
        assertEquals("target_btn", pasted.id)
        assertEquals("Target Button", pasted.name)
        assertEquals("#0F172A", pasted.appearance.backgroundColor)
        assertEquals(22, pasted.appearance.cornerRadius)
        assertEquals("#38BDF8", pasted.appearance.strokeColor)
        assertEquals(18, pasted.material.blurRadius)
    }

    @Test
    fun testSessionStateSerialization() {
        val session = UiStudioSessionState(
            selectedScreenKey = "result",
            selectedComponentKey = "score_card",
            selectedTab = "material",
            viewMode = "split",
            deviceWidthMode = "compact"
        )
        val gson = com.google.gson.Gson()
        val json = gson.toJson(session)
        assertNotNull(json)
        assertTrue(json.contains("score_card"))
        assertTrue(json.contains("material"))

        val restored = gson.fromJson(json, UiStudioSessionState::class.java)
        assertEquals("result", restored.selectedScreenKey)
        assertEquals("score_card", restored.selectedComponentKey)
        assertEquals("material", restored.selectedTab)
        assertEquals("split", restored.viewMode)
        assertEquals("compact", restored.deviceWidthMode)
    }
}
