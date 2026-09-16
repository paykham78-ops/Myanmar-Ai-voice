package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object AudioStorageHelper {

    fun getAudioDirectory(context: Context): File {
        val dir = File(context.filesDir, "audio")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createAudioFile(context: Context, extension: String = "wav"): File {
        val dir = getAudioDirectory(context)
        val filename = "myanmar_voice_${System.currentTimeMillis()}.$extension"
        return File(dir, filename)
    }

    fun shareAudio(context: Context, filePath: String, textSnippet: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "အသံဖိုင် ရှာမတွေ့ပါ", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Myanmar AI Voice:\n$textSnippet")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "အသံဖိုင် မျှဝေရန်"))
        } catch (e: Exception) {
            Toast.makeText(context, "မျှဝေရန် မအောင်မြင်ပါ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadAudio(context: Context, filePath: String): Boolean {
        val sourceFile = File(filePath)
        if (!sourceFile.exists()) {
            Toast.makeText(context, "အသံဖိုင် ရှာမတွေ့ပါ", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            val fileName = "Myanmar_Voice_${System.currentTimeMillis()}.wav"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/MyanmarAIVoice")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                    Toast.makeText(context, "အသံဖိုင်ကို Music/MyanmarAIVoice သို့ သိမ်းဆည်းပြီးပါပြီ", Toast.LENGTH_LONG).show()
                    return true
                }
            } else {
                @Suppress("DEPRECATION")
                val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MyanmarAIVoice")
                if (!publicDir.exists()) publicDir.mkdirs()
                val destFile = File(publicDir, fileName)
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(context, "အသံဖိုင်ကို သိမ်းဆည်းပြီးပါပြီ: ${destFile.name}", Toast.LENGTH_LONG).show()
                return true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "ဖိုင်သိမ်းဆည်းရန် မအောင်မြင်ပါ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
        return false
    }
}
