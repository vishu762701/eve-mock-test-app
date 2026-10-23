package com.eve.app.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EveOriginalActionIconTest {
    @Test fun genericSourceInformedIconsRemainIndependentAndTintable() {
        for (name in listOf("ic_delete", "ic_share", "ic_close", "ic_bookmark",
            "ic_bell", "ic_bell_off", "ic_notification", "ic_settings", "ic_edit", "ic_pin")) {
            val xml = File("src/main/res/drawable/" + name + ".xml").readText()
            assertTrue("Vector missing: " + name, xml.contains("<vector"))
            assertTrue("24dp intrinsic size lost: " + name,
                xml.contains("android:viewportWidth=\"24\"") &&
                xml.contains("android:viewportHeight=\"24\"") &&
                xml.contains("android:width=\"@dimen/design_icon_size\""))
            assertTrue("No independently authored notice: " + name,
                xml.contains("EVE-original generic Android action geometry"))
            assertTrue("Unexpected copyrighted art attribution: " + name,
                !xml.contains("Tabler Icons") && !xml.contains("Telegram-master"))
            assertTrue("Missing tintable colored geometry: " + name,
                xml.contains("android:fillColor=\"@color/accent\"") ||
                xml.contains("android:strokeColor=\"@color/accent\""))
        }
    }
}
