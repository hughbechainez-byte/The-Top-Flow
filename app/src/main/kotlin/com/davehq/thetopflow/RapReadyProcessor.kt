package com.davehq.thetopflow

import android.media.MediaPlayer
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Log
import kotlin.math.roundToInt

/**
 * Practical Rap Ready processing for phone recordings.
 *
 * Full RapReady One is a multi-stage C++ chain (HPF → expander → EQ → dual
 * compression → de-ess → saturation → LPF → limiter). On Android we apply a
 * safe, headphones-oriented approximation during *playback* using platform
 * AudioEffects attached to the MediaPlayer session:
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
    private var dynamics: DynamicsProcessing? = null
    private var attachedSessionId: Int = 0
    private var currentAmount: Float = 0f

    fun apply(player: MediaPlayer, amountPercent: Float) {
        val amount = amountPercent.coerceIn(0f, 100f)
        currentAmount = amount
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
        try {
            dynamics?.enabled = false
            dynamics?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        loudness = null
        dynamics = null
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val cfg = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    /*channelCount*/ 1,
                    /*preEqInUse*/ true, /*preEqBandCount*/ 3,
                    /*mbcInUse*/ true, /*mbcBandCount*/ 3,
                    /*postEqInUse*/ true, /*postEqBandCount*/ 3,
                    /*limiterInUse*/ true
                ).build()
                dynamics = DynamicsProcessing(0, sessionId, cfg).apply { enabled = true }
            } catch (e: Exception) {
                Log.d(TAG, "rap_ready dynamics_failed ${e.javaClass.simpleName}")
                dynamics = null
            }
        }
    }

    private fun configure(amount: Float) {
        val t = (amount / 100f).coerceIn(0f, 1f)
        // Strength curve: soft near zero, solid by 62, capped at 100
        val strength = if (t < 0.05f) 0f else ((t - 0.05f) / 0.95f).coerceIn(0f, 1f)

        equalizer?.let { eq ->
            try {
                val bands = eq.numberOfBands.toInt()
                // Mild HPF-ish cut on lowest band, presence boost mid-high, air on top
                for (i in 0 until bands) {
                    val millibel = when {
                        i == 0 -> (-180 * strength).roundToInt().toShort() // low cut
                        i == bands - 1 -> (120 * strength).roundToInt().toShort() // air
                        i >= bands - 3 -> (90 * strength).roundToInt().toShort() // presence
                        i in 1..2 -> (-40 * strength).roundToInt().toShort() // mild mud cut
                        else -> 0
                    }
                    val range = eq.getBandLevelRange()
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dynamics?.let { dp ->
                try {
                    // Light multiband compression + safety limiter
                    val ratio = 1f + (2.5f * strength)
                    val threshold = -18f + (6f * (1f - strength))
                    for (band in 0 until 3) {
                        val mbc = DynamicsProcessing.MbcBand(
                            true,
                            40f, // attack ms
                            80f, // release ms
                            ratio,
                            threshold,
                            1f, // knee
                            0f, // noiseGateThreshold
                            1f, // expanderRatio
                            0f, // preGain
                            0f  // postGain
                        )
                        dp.setMbcBandAllChannelsTo(band, mbc)
                    }
                    val limiter = DynamicsProcessing.Limiter(
                        true,
                        true,
                        1, // link group
                        5f, // attack
                        50f, // release
                        ratio.coerceAtMost(4f),
                        -2f, // threshold
                        0f // postGain
                    )
                    dp.setLimiterAllChannelsTo(limiter)
                    dp.enabled = strength > 0.01f
                } catch (_: Exception) {
                }
            }
        }
    }

    companion object {
        private const val TAG = "RapReady"
        const val DEFAULT_AMOUNT = 62f
    }
}
