package com.example.audio

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object WavHelper {

    fun ensureWavOrMp3File(rawData: ByteArray, targetFile: File): File {
        // Check if already RIFF WAV
        if (rawData.size >= 4 &&
            rawData[0] == 'R'.code.toByte() &&
            rawData[1] == 'I'.code.toByte() &&
            rawData[2] == 'F'.code.toByte() &&
            rawData[3] == 'F'.code.toByte()
        ) {
            targetFile.writeBytes(rawData)
            return targetFile
        }

        // Check if MP3 (ID3 or sync word 0xFFE0)
        if (rawData.size >= 3 &&
            ((rawData[0] == 'I'.code.toByte() && rawData[1] == 'D'.code.toByte() && rawData[2] == '3'.code.toByte()) ||
                    (rawData[0] == 0xFF.toByte() && (rawData[1].toInt() and 0xE0) == 0xE0))
        ) {
            targetFile.writeBytes(rawData)
            return targetFile
        }

        // Raw PCM (Gemini TTS default 24000Hz 16-bit mono PCM)
        val wavBytes = pcmToWav(rawData, sampleRate = 24000, channels = 1, bitsPerSample = 16)
        targetFile.writeBytes(wavBytes)
        return targetFile
    }

    fun pcmToWav(
        pcmData: ByteArray,
        sampleRate: Int = 24000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        return header + pcmData
    }

    /**
     * Fallback melodic acoustic voice chime generator in case offline and TTS not installed.
     */
    fun generateAcousticSampleWav(targetFile: File, durationSeconds: Double = 3.5, speedMultiplier: Float = 1.0f) {
        val sampleRate = 24000
        val effectiveDuration = (durationSeconds / speedMultiplier).coerceIn(1.0, 10.0)
        val numSamples = (effectiveDuration * sampleRate).toInt()
        val buffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

        // Musical speech formants frequencies (F1, F2) simulating pleasant speech cadence
        val notes = doubleArrayOf(261.63, 329.63, 392.00, 523.25, 440.0, 349.23)
        val noteDuration = numSamples / notes.size

        for (i in 0 until numSamples) {
            val noteIndex = (i / noteDuration).coerceAtMost(notes.size - 1)
            val freq = notes[noteIndex]
            val envelope = sin(PI * (i % noteDuration) / noteDuration)
            val sampleValue = (sin(2.0 * PI * freq * i / sampleRate) * envelope * 0.5 +
                    sin(4.0 * PI * freq * i / sampleRate) * envelope * 0.25)
            val shortVal = (sampleValue * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer.putShort(shortVal.toShort())
        }

        val pcm = buffer.array()
        val wav = pcmToWav(pcm, sampleRate = sampleRate)
        targetFile.writeBytes(wav)
    }
}
