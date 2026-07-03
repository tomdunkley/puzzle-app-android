package com.tomdunkley.dailypuzzles.audio

import android.media.AudioManager
import android.media.ToneGenerator

/** Short synthesized tones (no audio asset files needed) for in-game actions, shared
 * across Words and Numbers. Uses the system tone generator rather than bundled sound
 * files -- there's no asset pipeline for audio in this app yet, and these built-in
 * tones are enough to distinguish a tap from a correct/incorrect/already-done result.
 */
object SoundFeedback {

    private var toneGenerator: ToneGenerator? = null

    fun init() {
        toneGenerator = runCatching { ToneGenerator(AudioManager.STREAM_SYSTEM, 70) }.getOrNull()
    }

    /** A light tick for selecting a tile, choosing an operator, undo, reset, etc. */
    fun tap() = play(ToneGenerator.TONE_PROP_BEEP, 40)

    /** A found word, a confirmed combine, or hitting the target exactly. */
    fun correct() = play(ToneGenerator.TONE_PROP_ACK, 150)

    /** An attempted word that isn't in the dictionary. */
    fun incorrect() = play(ToneGenerator.TONE_PROP_NACK, 150)

    /** A word that's valid but was already found this round. */
    fun duplicate() = play(ToneGenerator.TONE_PROP_BEEP2, 100)

    private fun play(tone: Int, durationMs: Int) {
        runCatching { toneGenerator?.startTone(tone, durationMs) }
    }
}
