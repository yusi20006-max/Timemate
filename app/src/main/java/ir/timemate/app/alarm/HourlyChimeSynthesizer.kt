package ir.timemate.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object HourlyChimeSynthesizer {

    private const val PREF_NAME = "timemate_chime_prefs"
    private const val KEY_ENABLED = "hourly_chime_enabled"
    private const val KEY_START_HOUR = "hourly_chime_start"
    private const val KEY_END_HOUR = "hourly_chime_end"

    fun isHourlyChimeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, true) // Default enabled (7 AM to 11 PM)
    }

    fun setHourlyChimeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) {
            AlarmScheduler.scheduleNextHourlyChime(context)
        } else {
            AlarmScheduler.cancelHourlyChime(context)
        }
    }

    fun getStartHour(context: Context): Int =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_START_HOUR, 7)

    fun getEndHour(context: Context): Int =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_END_HOUR, 23)

    /**
     * Synthesizes a pleasant, crystal-clear two-tone "Ding-Ding" chime:
     * Part 1 (Ding 1): High bell chime (1318.5 Hz - E6) for 170ms
     * Gap: 60ms silence
     * Part 2 (Ding 2): Warm resonant bell chime (1046.5 Hz - C6) for 320ms
     */
    fun playDingDing() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val sampleRate = 44100
                val ding1DurationMs = 170
                val gapDurationMs = 60
                val ding2DurationMs = 320

                val totalSamples = (sampleRate * (ding1DurationMs + gapDurationMs + ding2DurationMs)) / 1000
                val samples = ShortArray(totalSamples)

                val ding1Samples = (sampleRate * ding1DurationMs) / 1000
                val gapSamples = (sampleRate * gapDurationMs) / 1000
                val ding2Start = ding1Samples + gapSamples

                // First Ding (E6 + subtle harmonic)
                val freq1 = 1318.5
                for (i in 0 until ding1Samples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = exp(-9.5 * t) * (1.0 - exp(-220.0 * t))
                    val wave = 0.75 * sin(2.0 * PI * freq1 * t) + 0.25 * sin(2.0 * PI * (freq1 * 2.0) * t)
                    samples[i] = (wave * envelope * Short.MAX_VALUE * 0.85).toInt().toShort()
                }

                // Second Ding (C6 + subtle harmonic)
                val freq2 = 1046.5
                for (i in ding2Start until totalSamples) {
                    val localIdx = i - ding2Start
                    val t = localIdx.toDouble() / sampleRate
                    val envelope = exp(-6.5 * t) * (1.0 - exp(-220.0 * t))
                    val wave = 0.75 * sin(2.0 * PI * freq2 * t) + 0.25 * sin(2.0 * PI * (freq2 * 2.0) * t)
                    samples[i] = (wave * envelope * Short.MAX_VALUE * 0.90).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()

                kotlinx.coroutines.delay(750L)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
