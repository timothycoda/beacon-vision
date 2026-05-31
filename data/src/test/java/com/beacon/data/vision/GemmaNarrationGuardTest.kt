package com.beacon.data.vision

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GemmaNarrationGuardTest {

    @Test
    fun acceptsHausaOpeners() {
        assertTrue(GemmaNarrationGuard.isLikelyHausa("A gaban ka akwai kujera da tebur."))
    }

    @Test
    fun rejectsEnglishOpeners() {
        assertFalse(GemmaNarrationGuard.isLikelyHausa("Ahead of you is a chair and a table."))
    }
}
