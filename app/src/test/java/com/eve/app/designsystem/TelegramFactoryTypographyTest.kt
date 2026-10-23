package com.eve.app.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Factory config and explicit-only runtime overrides, without Android-context mocks. */
class TelegramFactoryTypographyTest {
    private fun source(path: String) = File("src/main/java/com/eve/app/" + path).readText()

    @Test fun factoryUsesTelegramNotRetiredIosProfile() {
        val m = source("typography/TypographyModels.kt")
        val r = source("typography/TypographyRepository.kt")
        val resolver = source("typography/TypographyResolver.kt")
        assertTrue(m.contains("val TELEGRAM_NATIVE = TypographyPreset("))
        assertTrue(m.contains("id = \"telegram_native\""))
        assertTrue(m.contains("val presetId: String = \"telegram_native\""))
        assertTrue(m.contains("val fontFamily: String = \"roboto\""))
        assertTrue(m.contains("?: TELEGRAM_NATIVE"))
        assertTrue(r.contains("val preset = TypographyPreset.TELEGRAM_NATIVE"))
        assertTrue(source("typography/TypographyApplier.kt").contains("if (config.screenOverrides.isEmpty() &&"))
        assertTrue(resolver.contains("val defaultPreset = TypographyPreset.TELEGRAM_NATIVE"))
        assertTrue(resolver.contains("fallbackFamily = \"roboto\""))
        assertTrue(r.contains("val parsed = gson.fromJson(json, TypographyConfig::class.java)"))
        assertTrue(r.contains("if (!isTestActive)"))
    }

    @Test fun existingTelegramXmlMediumNeverDowngradedByDefaultProfile() {
        val m = source("typography/TypographyApplier.kt")
        val f = source("util/FontManager.kt")
        assertTrue(m.contains("if (view.tag == \"eve_telegram_type\") return"))
        assertTrue(m.contains("if (hasExplicitOverride)"))
        assertTrue(m.contains("view.paint.isFakeBoldText = false"))
        assertTrue(!m.contains("view.typeface = FontManager.getTypeface(view.context, family, weight)"))
        assertTrue(f.contains("?: ROBOTO"))
        assertTrue(f.contains("R.font.telegram_roboto_medium"))
    }
}
