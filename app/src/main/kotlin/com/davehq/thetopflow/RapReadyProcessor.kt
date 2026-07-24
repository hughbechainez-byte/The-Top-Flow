package com.davehq.thetopflow

import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import kotlin.math.roundToInt

/**
 * Practical Rap Ready processing for phone recordings.
 *
 * Applies a safe headphones-oriented approximation during *playback*
 * using platform AudioEffects attached to the MediaPlayer session:
 *
 *   amount 0   → dry (effects disabled)
 *   amount 62  → RAP DEFAULT (mild presence + controlled loudness)
 *   amount 100 → stronger presence / air / loudness within fixed safety caps
 *
 * Effects are released when playback stops. Never mutates the source file.
 */
class RapReadyProcessor {

    private var equalizer: Equalizer? = null
    private var loudness: LoudnessEnhancer? = null
    private var attachedSessionId: Int = 0

    fun apply(player: MediaPlayer, amountPercent: Float) {
        val amount = amountPercent.coerceIn(0f, 100f)
        val sessionId = try {
            player.audioSessionId
        } catch (_: Exception) {
            0
        }
        if (sessionId == 0) {
            Log.d(TAG, "rap_ready skip reason=no_session")
            return
        }
        if (amount < 0.5f) {
            release()
            Log.d(TAG, "rap_ready dry amount=0")
            return
        }
        if (sessionId != attachedSessionId) {
            release()
            attachedSessionId = sessionId
            attachEffects(sessionId)
        }
        configure(amount)
        Log.d(TAG, "rap_ready applied amount=${amount.roundToInt()} session=$sessionId")
    }

    fun release() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (_: Exception) {
        }
        try {
            loudness?.enabled = false
            loudness?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        loudness = null
        attachedSessionId = 0
    }

    private fun attachEffects(sessionId: Int) {
        try {
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.d(TAG, "rap_ready eq_failed ${e.javaClass.simpleName}")
            equalizer = null
        }
        try {
            loudness = LoudnessEnhancer(sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.d(TAG, "rap_ready loudness_failed ${e.javaClass.simpleName}")
            loudness = null
        }
    }

    private fun configure(amount: Float) {
        val t = (amount / 100f).coerceIn(0f, 1f)
        // Soft near zero, solid by 62, capped at 100
        val strength = if (t < 0.05f) 0f else ((t - 0.05f) / 0.95f).coerceIn(0f, 1f)

        equalizer?.let { eq ->
            try {
                val bands = eq.numberOfBands.toInt()
                if (bands <= 0) return@let
                val range = eq.bandLevelRange
                for (i in 0 until bands) {
                    // millibels: mild low cut, mud dip, presence, air
                    val millibel = when {
                        i == 0 -> (-180 * strength).roundToInt()
                        i == bands - 1 -> (120 * strength).roundToInt()
                        i >= bands - 3 -> (90 * strength).roundToInt()
                        i in 1..2 -> (-40 * strength).roundToInt()
                        else -> 0
                    }.toShort()
                    val clamped = millibel.coerceIn(range[0], range[1])
                    eq.setBandLevel(i.toShort(), clamped)
                }
                eq.enabled = strength > 0.01f
            } catch (_: Exception) {
            }
        }

        loudness?.let { le ->
            try {
                // Max ~6 dB at full knob — safe for headphones
                val gainMb = (600 * strength).roundToInt()
                le.setTargetGain(gainMb)
                le.enabled = strength > 0.01f
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val TAG = "RapReady"
        const val DEFAULT_AMOUNT = 62f
    }
}
