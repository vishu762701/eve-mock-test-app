package com.eve.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class EveShapesTest {
    @Test fun concentricUsesInsetAndClampsSmallParents() {
        assertEquals(14f, EveShapes.concentric(26f, 12f), 0f)
        assertEquals(18f, EveShapes.concentric(34f, 16f), 0f)
        assertEquals(4f, EveShapes.concentric(10f, 16f), 0f)
        assertEquals(4f, EveShapes.concentric(0f, 0f), 0f)
    }
    @Test fun capsuleTracksMeasuredHeight() {
        assertEquals(24f, EveShapes.capsule(48f), 0f)
        assertEquals(35.5f, EveShapes.capsule(71f), 0f)
    }
}
