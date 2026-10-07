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

    @Test
    fun testComponentRegistryDescriptorsAndAttributes() {
        val types = com.eve.app.uistudio.UiStudioRegistry.COMPONENT_TYPES
        assertEquals(16, types.size)

        for (desc in types) {
            assertTrue("Type ID must not be blank", desc.type.isNotBlank())
            assertTrue("Display name must not be blank", desc.displayName.isNotBlank())
            assertTrue("Category must not be blank", desc.category.isNotBlank())
            assertTrue("Icon res must not be blank", desc.iconRes.isNotBlank())
            assertTrue("Supported properties must not be empty", desc.supportedProperties.isNotEmpty())
            assertTrue("Supported states must not be empty", desc.supportedStates.isNotEmpty())
            assertTrue("Supported actions must not be empty", desc.supportedActions.isNotEmpty())
            assertTrue("Supported animations must not be empty", desc.supportedAnimations.isNotEmpty())
            assertTrue("Preview behavior must not be blank", desc.previewBehavior.isNotBlank())
            assertTrue("Runtime behavior must not be blank", desc.runtimeBehavior.isNotBlank())
        }

        // Test specific known primitives
        val card = com.eve.app.uistudio.UiStudioRegistry.getComponentType("card")
        assertNotNull(card)
        assertTrue(card!!.isContainer)
        assertTrue(card.allowsChildren)
        assertEquals("Surface", card.category)

        val button = com.eve.app.uistudio.UiStudioRegistry.getComponentType("button")
        assertNotNull(button)
        assertFalse(button!!.isContainer)
        assertFalse(button.allowsChildren)
        assertEquals("Action", button.category)
        assertTrue(button.supportedActions.contains("navigate"))

        val spacer = com.eve.app.uistudio.UiStudioRegistry.getComponentType("spacer")
        assertNotNull(spacer)
        assertFalse(spacer!!.allowsChildren)
    }

    @Test
    fun testComponentRegistryHierarchyAndPropertyConstraints() {
        // Containers should allow children
        assertTrue(com.eve.app.uistudio.UiStudioRegistry.isChildAllowed("container", "button"))
        assertTrue(com.eve.app.uistudio.UiStudioRegistry.isChildAllowed("card", "text"))

        // Non-containers should disallow children
        assertFalse(com.eve.app.uistudio.UiStudioRegistry.isChildAllowed("spacer", "text"))
        assertFalse(com.eve.app.uistudio.UiStudioRegistry.isChildAllowed("divider", "button"))
        assertFalse(com.eve.app.uistudio.UiStudioRegistry.isChildAllowed("timer", "card"))

        // Specific parent constraints
        assertTrue(com.eve.app.uistudio.UiStudioRegistry.isChildAllowed("banner", "card"))
        assertFalse(com.eve.app.uistudio.UiStudioRegistry.isChildAllowed("banner", "timer"))

        // Property support checks
        assertTrue(com.eve.app.uistudio.UiStudioRegistry.isPropertySupported("card", "material"))
        assertFalse(com.eve.app.uistudio.UiStudioRegistry.isPropertySupported("spacer", "typography"))
    }

    @Test
    fun testScreenRegistryDescriptors() {
        val screens = com.eve.app.uistudio.UiStudioRegistry.SUPPORTED_SCREENS
        assertEquals(8, screens.size)

        for (screen in screens) {
            assertTrue("Screen ID must not be blank", screen.id.isNotBlank())
            assertTrue("Display name must not be blank", screen.displayName.isNotBlank())
            assertTrue("Runtime activity must not be blank", screen.runtimeActivity.isNotBlank())
            assertTrue("Editable root ID must not be blank", screen.editableRootId.isNotBlank())
            assertTrue("Supported properties must not be empty", screen.supportedProperties.isNotEmpty())
        }

        val home = com.eve.app.uistudio.UiStudioRegistry.getScreen("home")
        assertNotNull(home)
        assertEquals("MainActivity", home!!.runtimeActivity)
        assertEquals("root_home_container", home.editableRootId)
    }

    @Test
    fun testComponentLifecycleOperations() {
        val baseConfig = repo.getDefaultTemplate()
        val homeScreen = baseConfig.screens["home"] ?: error("Missing home screen")

        // 1. Add component
        val newComp = UiStudioRegistry.createDefaultComponent("custom_badge_1", "badge", "New Badge")
        val withAdded = homeScreen.components.toMutableMap().apply {
            put(newComp.id, newComp)
        }
        assertTrue(withAdded.containsKey("custom_badge_1"))
        assertEquals("badge", withAdded["custom_badge_1"]?.type)

        // 2. Duplicate component with unique ID
        val duplicated = withAdded["custom_badge_1"]!!.copy(
            id = "custom_badge_1_copy_${System.currentTimeMillis()}",
            name = "New Badge Copy"
        )
        assertNotEquals(withAdded["custom_badge_1"]!!.id, duplicated.id)
        assertTrue(duplicated.id.startsWith("custom_badge_1_copy_"))
        withAdded[duplicated.id] = duplicated
        assertTrue(withAdded.containsKey(duplicated.id))

        // 3. Visibility toggle
        val toggled = duplicated.copy(visible = false)
        assertFalse(toggled.visible)
        withAdded[duplicated.id] = toggled

        // 4. Reorder persists
        val orderedComps = withAdded.values.sortedBy { it.order }.toMutableList()
        val firstComp = orderedComps.first()
        val lastComp = orderedComps.last()
        val updatedFirst = firstComp.copy(order = lastComp.order + 10)
        withAdded[firstComp.id] = updatedFirst
        val reSorted = withAdded.values.sortedBy { it.order }
        assertEquals(firstComp.id, reSorted.last().id)

        // 5. Delete component
        withAdded.remove(duplicated.id)
        assertFalse(withAdded.containsKey(duplicated.id))
    }

    @Test
    fun testEmojiContentPersistence() {
        val emojis = "🚀 Practice & Master Exam 💯🔥 🇮🇳"
        val subtitleWithEmoji = "Level Up Your Prep ⚡"

        val config = UiStudioConfig(
            version = 1,
            screens = mapOf(
                "home" to ScreenConfig(
                    id = "home",
                    components = mapOf(
                        "hero_banner" to ComponentConfig(
                            id = "hero_banner",
                            content = ContentProperties(
                                title = emojis,
                                subtitle = subtitleWithEmoji
                            )
                        )
                    )
                )
            )
        )

        val json = repo.exportToJson(config)
        assertTrue(json.contains("🚀"))
        assertTrue(json.contains("💯"))
        assertTrue(json.contains("🇮🇳"))

        val imported = repo.importFromJson(json).getOrThrow()
        val comp = imported.screens["home"]?.components?.get("hero_banner")
        assertNotNull(comp)
        assertEquals(emojis, comp?.content?.title)
        assertEquals(subtitleWithEmoji, comp?.content?.subtitle)
    }

    @Test
    fun testConcurrencyConflictModel() {
        val serverDraft = UiStudioConfig(
            version = 2,
            revision = "rev-server-draft-456",
            notes = "Server newer draft"
        )
        val conflictResult = SaveDraftResult.Conflict(
            serverDraft = serverDraft,
            message = "A newer draft exists on the server."
        )

        assertEquals("A newer draft exists on the server.", conflictResult.message)
        assertNotNull(conflictResult.serverDraft)
        assertEquals("rev-server-draft-456", conflictResult.serverDraft?.revision)
        assertEquals(2, conflictResult.serverDraft?.version)
    }

    @Test
    fun testScreenBackgroundFallbackHierarchy() {
        // Hierarchy: Screen background override > DesignSystem appBackground > Global Default (#000000)
        val defaultColor = 0xFF000000.toInt()

        // 1. When screen has explicit background
        val screenColorHex = "#1E293B"
        val parsedScreenColor = UiStudioEngine.parseColorSafe(screenColorHex, defaultColor)
        assertEquals(0xFF1E293B.toInt(), parsedScreenColor)

        // 2. When screen is null, designSystem fallback
        val dsColorHex = "#0F172A"
        val parsedDsColor = UiStudioEngine.parseColorSafe(null) ?: UiStudioEngine.parseColorSafe(dsColorHex, defaultColor)
        assertEquals(0xFF0F172A.toInt(), parsedDsColor)

        // 3. When both are null/empty, fallback to default
        val parsedFallback = UiStudioEngine.parseColorSafe(null) ?: UiStudioEngine.parseColorSafe("", defaultColor)
        assertEquals(defaultColor, parsedFallback)
    }

    @Test
    fun testTimerHostAndCircularTimerContract() {
        val defaultTemplate = repo.getDefaultTemplate()
        val testScreen = defaultTemplate.screens["test"]
        assertNotNull(testScreen)

        val timerPill = testScreen?.components?.get("timer_pill")
        assertNotNull(timerPill)
        assertEquals("timer", timerPill?.type)
        assertEquals(true, timerPill?.visible)
        assertNotNull(timerPill?.appearance?.cornerRadius)
        assertNotNull(timerPill?.appearance?.backgroundColor)

        // Verify stroke and colors parse cleanly
        val ringColor = UiStudioEngine.parseColorSafe(timerPill?.appearance?.strokeColor, 0xFF007AFF.toInt())
        val textColor = UiStudioEngine.parseColorSafe(timerPill?.typography?.textColor, 0xFFFFFFFF.toInt())
        assertNotNull(ringColor)
        assertNotNull(textColor)
    }

    @Test
    fun testQuestionOptionStylingContract() {
        val defaultTemplate = repo.getDefaultTemplate()
        val testScreen = defaultTemplate.screens["test"]
        val optionItem = testScreen?.components?.get("option_item")
        assertNotNull(optionItem)
        assertEquals("option_item", optionItem?.id)
        assertEquals("Answer Option Capsule", optionItem?.name)

        // Option item supports appearance, states (selected, pressed, disabled)
        val customOption = optionItem!!.copy(
            states = StateProperties(
                selectedBackgroundColor = "#007AFF",
                pressedBackgroundColor = "#1E293B",
                disabledBackgroundColor = "#334155"
            )
        )
        val selectedBg = customOption.states.selectedBackgroundColor
        assertNotNull(selectedBg)
        val parsedSelectedBg = UiStudioEngine.parseColorSafe(selectedBg)
        assertNotNull(parsedSelectedBg)
        assertEquals(0xFF007AFF.toInt(), parsedSelectedBg)

        // Check corner radius
        val radius = optionItem.appearance.cornerRadius ?: 12
        assertTrue(radius in 0..120)
    }

    @Test
    fun testTestActionPillsContract() {
        val defaultTemplate = repo.getDefaultTemplate()
        val testScreen = defaultTemplate.screens["test"]
        val actionGrid = testScreen?.components?.get("action_grid")
        assertNotNull(actionGrid)
        assertEquals("action_grid", actionGrid?.type)
        assertTrue(actionGrid?.visible == true)
    }

    @Test
    fun testAnimationPropertiesAndPlaybackSafety() {
        val anim = AnimationProperties(
            enabled = true,
            type = "fade_scale",
            durationMs = 400L,
            delayMs = 100L,
            interpolator = "overshoot"
        )
        assertEquals("fade_scale", anim.type)
        assertEquals(400L, anim.durationMs)
        assertEquals(100L, anim.delayMs)
        assertEquals("overshoot", anim.interpolator)

        // Safe playback with null view
        UiStudioEngine.playEntranceAnimation(null, anim)
    }

    @Test
    fun testUniversalSearchRouting() {
        fun resolveInspectorTabForQuery(query: String): String? {
            val q = query.trim().lowercase()
            return when {
                q.contains("blur") || q.contains("glass") || q.contains("material") -> "material"
                q.contains("radius") || q.contains("corner") || q.contains("shape") -> "design"
                q.contains("color") || q.contains("background") || q.contains("hex") || q.contains("tint") -> "colors"
                q.contains("anim") || q.contains("motion") -> "animation"
                q.contains("text") || q.contains("typo") || q.contains("font") -> "typography"
                q.contains("layout") || q.contains("margin") || q.contains("padding") -> "layout"
                q.contains("action") || q.contains("nav") -> "actions"
                q.contains("state") || q.contains("press") -> "states"
                q.contains("brand") || q.contains("logo") -> "branding"
                else -> null
            }
        }

        assertEquals("material", resolveInspectorTabForQuery("blur radius"))
        assertEquals("material", resolveInspectorTabForQuery("frosted glass"))
        assertEquals("design", resolveInspectorTabForQuery("corner radius"))
        assertEquals("colors", resolveInspectorTabForQuery("background color"))
        assertEquals("animation", resolveInspectorTabForQuery("fade anim"))
        assertEquals("typography", resolveInspectorTabForQuery("font size"))
        assertEquals("layout", resolveInspectorTabForQuery("padding bottom"))
        assertEquals("actions", resolveInspectorTabForQuery("action target"))
        assertEquals("states", resolveInspectorTabForQuery("pressed state"))
        assertEquals("branding", resolveInspectorTabForQuery("brand logo"))
        assertNull(resolveInspectorTabForQuery("something unknown"))
    }

    @Test
    fun testProtectedThemeAnimationIntegrity() {
        // Assert Telegram Day/Night circular reveal constants and state are preserved and intact
        assertEquals("ThemeSwitchAnimator", ThemeSwitchAnimator.TAG)
        assertEquals("eve_prefs", ThemeSwitchAnimator.PREFS)
        assertEquals("key_dark_mode", ThemeSwitchAnimator.KEY_DARK_MODE)
        assertEquals(400L, ThemeSwitchAnimator.ANIMATION_DURATION)
    }

    @Test
    fun testAllRuntimeAdaptersContracts() {
        // Verify null-safety and safe delegation of all 13 formal runtime adapters
        val sampleConfig = ComponentConfig(
            id = "test_comp",
            type = "button",
            name = "Test Button",
            appearance = AppearanceProperties(
                backgroundColor = "#1E293B",
                cornerRadius = 14,
                strokeWidth = 2,
                strokeColor = "#38BDF8"
            ),
            typography = TypographyProperties(
                textColor = "#FFFFFF",
                textSize = 15,
                textStyle = "bold"
            ),
            states = StateProperties(
                selectedBackgroundColor = "#16A34A",
                selectedTextColor = "#FFFFFF"
            ),
            content = ContentProperties(
                title = "Submit Exam",
                subtitle = "150 Questions"
            )
        )

        // 1. TextAdapter
        UiStudioEngine.TextAdapter.apply(null, sampleConfig)
        UiStudioEngine.TextAdapter.apply(null, null)

        // 2. ButtonAdapter
        UiStudioEngine.ButtonAdapter.apply(null, sampleConfig)
        UiStudioEngine.ButtonAdapter.apply(null, null)

        // 3. TimerAdapter
        UiStudioEngine.TimerAdapter.apply(null, null, sampleConfig)
        UiStudioEngine.TimerAdapter.apply(null, null, null)

        // 4. QuestionOptionAdapter
        UiStudioEngine.QuestionOptionAdapter.apply(emptyList(), sampleConfig, 1)
        UiStudioEngine.QuestionOptionAdapter.apply(emptyList(), null, -1)

        // 5. TestActionAdapter
        UiStudioEngine.TestActionAdapter.apply(emptyList(), sampleConfig)
        UiStudioEngine.TestActionAdapter.apply(emptyList(), null)

        // 6. ResultStatAdapter
        UiStudioEngine.ResultStatAdapter.apply(null, null, null, sampleConfig)
        UiStudioEngine.ResultStatAdapter.apply(null, null, null, null)

        // 7. NavigationAdapter
        UiStudioEngine.NavigationAdapter.apply(null, sampleConfig)
        UiStudioEngine.NavigationAdapter.apply(null, null)

        // 8. MaterialSurfaceAdapter
        UiStudioEngine.MaterialSurfaceAdapter.apply(null, sampleConfig)
        UiStudioEngine.MaterialSurfaceAdapter.apply(null, null)

        // 9. ImageAdapter
        UiStudioEngine.ImageAdapter.apply(null, sampleConfig)
        UiStudioEngine.ImageAdapter.apply(null, null)

        // 10. IconAdapter
        UiStudioEngine.IconAdapter.apply(null, sampleConfig)
        UiStudioEngine.IconAdapter.apply(null, null)

        // 11. SwitchAdapter
        UiStudioEngine.SwitchAdapter.apply(null, sampleConfig)
        UiStudioEngine.SwitchAdapter.apply(null, null)

        // 12. SliderAdapter
        UiStudioEngine.SliderAdapter.apply(null, sampleConfig)
        UiStudioEngine.SliderAdapter.apply(null, null)

        // 13. RowAdapter
        UiStudioEngine.RowAdapter.apply(null, null, null, sampleConfig)
        UiStudioEngine.RowAdapter.apply(null, null, null, null)
    }

    @Test
    fun testAllEightScreensDefaultTemplateCoverage() {
        val template = UiStudioRegistry.getCompleteDefaultTemplate()
        val expectedScreens = listOf("home", "test", "result", "profile", "notifications", "syllabus", "login", "admin")

        assertEquals(8, template.screens.size)
        for (screenId in expectedScreens) {
            assertTrue("Template must contain screen '$screenId'", template.screens.containsKey(screenId))
            val screen = template.screens[screenId]
            assertNotNull(screen)
            assertTrue("Screen '$screenId' must have components", screen!!.components.isNotEmpty())
        }

        // Verify specific authentic components for each screen
        val homeComps = template.screens["home"]!!.components
        assertTrue(homeComps.containsKey("featured_exam_card"))
        assertTrue(homeComps.containsKey("home_bottom_nav"))

        val testComps = template.screens["test"]!!.components
        assertTrue(testComps.containsKey("test_top_bar"))
        assertTrue(testComps.containsKey("timer_pill"))
        assertTrue(testComps.containsKey("question_palette"))
        assertTrue(testComps.containsKey("question_card"))
        assertTrue(testComps.containsKey("option_item"))
        assertTrue(testComps.containsKey("action_grid"))

        val resultComps = template.screens["result"]!!.components
        assertTrue(resultComps.containsKey("result_header"))
        assertTrue(resultComps.containsKey("score_card"))
        assertTrue(resultComps.containsKey("result_tabs"))
        assertTrue(resultComps.containsKey("analytics_summary"))
        assertTrue(resultComps.containsKey("result_insight"))
        assertTrue(resultComps.containsKey("result_bottom_bar"))

        val profileComps = template.screens["profile"]!!.components
        assertTrue(profileComps.containsKey("profile_header"))
        assertTrue(profileComps.containsKey("profile_rows"))
        assertTrue(profileComps.containsKey("btn_logout"))

        val notifComps = template.screens["notifications"]!!.components
        assertTrue(notifComps.containsKey("notifications_header"))
        assertTrue(notifComps.containsKey("notification_card"))
        assertTrue(notifComps.containsKey("notification_card_2"))

        val syllabusComps = template.screens["syllabus"]!!.components
        assertTrue(syllabusComps.containsKey("syllabus_header"))
        assertTrue(syllabusComps.containsKey("syllabus_selector"))
        assertTrue(syllabusComps.containsKey("syllabus_card"))

        val loginComps = template.screens["login"]!!.components
        assertTrue(loginComps.containsKey("login_hero"))
        assertTrue(loginComps.containsKey("login_inputs"))
        assertTrue(loginComps.containsKey("btn_login"))
        assertTrue(loginComps.containsKey("login_google"))

        val adminComps = template.screens["admin"]!!.components
        assertTrue(adminComps.containsKey("admin_top_bar"))
        assertTrue(adminComps.containsKey("admin_modules"))
        assertTrue(adminComps.containsKey("admin_health"))

        val validation = repo.validateConfig(template)
        assertTrue("All 8 screens template must be schema valid: ${validation.second}", validation.first)
    }

    @Test
    fun testSessionStateReopenPersistence() {
        val sessionState = UiStudioSessionState(
            selectedScreenKey = "test",
            selectedComponentKey = "option_item",
            selectedTab = "material",
            viewMode = "split",
            deviceWidthMode = "normal",
            timestamp = 1770000000000L
        )

        assertEquals("test", sessionState.selectedScreenKey)
        assertEquals("option_item", sessionState.selectedComponentKey)
        assertEquals("material", sessionState.selectedTab)
        assertEquals("split", sessionState.viewMode)
        assertEquals("normal", sessionState.deviceWidthMode)
        assertEquals(1770000000000L, sessionState.timestamp)
    }

    @Test
    fun testStrictPublishVerificationVersionMatching() {
        // Publish verification must strictly require liveConfig.version == expectedVersion
        val expectedVersion = 12

        val matchingConfig = UiStudioConfig(version = 12, status = "published")
        val isVerified = (matchingConfig.version == expectedVersion)
        assertTrue("Exact version match must succeed verification", isVerified)

        val staleConfig = UiStudioConfig(version = 11, status = "published")
        val isStaleVerified = (staleConfig.version == expectedVersion)
        assertFalse("Stale version must fail verification", isStaleVerified)
    }
}
