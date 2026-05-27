package com.catamsp.Daemon.preferences.theme

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object FontManager {

    private const val CUSTOM_FONT_DIR = "custom_fonts"
    private val typefaceCache = mutableMapOf<String, Typeface>()

    /**
     * Copies a font file from a Uri to the app's internal private storage.
     * @return The filename of the imported font, or null if failed.
     */
    fun importFont(context: Context, uri: Uri): String? {
        return try {
            val fileName = getFileName(context, uri) ?: "custom_font_${System.currentTimeMillis()}.ttf"
            if (!fileName.endsWith(".ttf", true) && !fileName.endsWith(".otf", true)) {
                return null
            }

            val fontDir = File(context.filesDir, CUSTOM_FONT_DIR)
            if (!fontDir.exists()) fontDir.mkdirs()

            val destFile = File(fontDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Clear cache in case an existing font was overwritten
            typefaceCache.remove(fileName)
            
            fileName
        } catch (e: Exception) {
            Log.e("FontManager", "Failed to import font: ${e.message}")
            null
        }
    }

    /**
     * Scans the internal custom fonts directory and returns a list of filenames.
     */
    fun getCustomFontNames(context: Context): List<String> {
        val fontDir = File(context.filesDir, CUSTOM_FONT_DIR)
        if (!fontDir.exists()) return emptyList()
        return fontDir.listFiles()?.map { it.name }?.sorted() ?: emptyList()
    }

    /**
     * Gets a Typeface by name (either built-in enum name or custom filename).
     * Now performs a case-insensitive search to support formatted UI labels.
     */
    fun getTypeface(context: Context, fontName: String): Typeface {
        // 1. Check if it's a built-in font (Case-Insensitive Enum lookup)
        Font.entries.find { it.name.equals(fontName, ignoreCase = true) }?.let {
            return it.getTypeface(context)
        }

        // 2. Check Memory Cache (Case-Insensitive)
        typefaceCache.entries.find { it.key.equals(fontName, ignoreCase = true) }?.value?.let { return it }

        // 3. Load from Disk Cache (Case-Insensitive File Search)
        return try {
            val fontDir = File(context.filesDir, CUSTOM_FONT_DIR)
            val fontFile = fontDir.listFiles()?.find { it.name.equals(fontName, ignoreCase = true) }
            
            if (fontFile != null && fontFile.exists()) {
                val tf = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // CRITICAL FIX: Use Builder instead of createFromFile for modern safety
                    Typeface.Builder(fontFile).build()
                } else {
                    @Suppress("DEPRECATION")
                    Typeface.createFromFile(fontFile)
                }
                
                if (tf != null) {
                    typefaceCache[fontFile.name] = tf // Cache with original case
                    tf
                } else {
                    Typeface.DEFAULT
                }
            } else {
                Typeface.DEFAULT
            }
        } catch (e: Exception) {
            Log.e("FontManager", "Failed to load custom font $fontName: ${e.message}")
            Typeface.DEFAULT
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    fun getDisplayName(fileName: String): String {
        // If it's a built-in enum like SYSTEM_DEFAULT, just return it properly formatted
        if (!fileName.contains(".")) {
            return fileName.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
        }
        
        return fileName
            .substringBeforeLast(".") // Strip .ttf or .otf
            .replace(Regex("([a-z])([A-Z]+)"), "$1 $2") // Split CamelCase
            .replace(Regex("[-_]"), " ") // Replace hyphens/underscores
            .split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
