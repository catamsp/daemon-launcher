package com.catamsp.Daemon.apps

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.catamsp.Daemon.ui.list.apps.UniversalSearchResult

object UniversalSearchProvider {

    fun getMathResults(query: String): List<UniversalSearchResult> {
        val results = mutableListOf<UniversalSearchResult>()
        if (query.isEmpty()) {
            results.add(UniversalSearchResult(id = "math_empty", title = "Enter a math expression") {})
        } else {
            try {
                val res = eval(query)
                // Format the output: drop .0 for whole numbers
                val formatted = if (res == res.toLong().toDouble()) {
                    res.toLong().toString()
                } else {
                    res.toString()
                }
                results.add(UniversalSearchResult(id = "math_res", title = formatted) {})
            } catch (e: Exception) {
                // If it's incomplete or invalid, just show the query being typed
                results.add(UniversalSearchResult(id = "math_partial", title = "= $query") {})
            }
        }
        return results
    }

    // A robust, lightweight recursive descent parser for math expressions
    private fun eval(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0
            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) x /= parseFactor() // division
                    else if (eat('%'.code)) x %= parseFactor() // modulo
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus
                var x: Double
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func = str.substring(startPos, pos)
                    x = parseFactor()
                    x = when (func) {
                        "sqrt" -> Math.sqrt(x)
                        "sin" -> Math.sin(Math.toRadians(x))
                        "cos" -> Math.cos(Math.toRadians(x))
                        "tan" -> Math.tan(Math.toRadians(x))
                        "log" -> Math.log10(x)
                        "ln" -> Math.log(x)
                        else -> throw RuntimeException("Unknown function: $func")
                    }
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                if (eat('^'.code)) x = Math.pow(x, parseFactor()) // exponentiation
                return x
            }
        }.parse()
    }

    fun getSettingsResults(context: Context, query: String): List<UniversalSearchResult> {
        val settingsMap = mapOf(
            "Wi-Fi" to Settings.ACTION_WIFI_SETTINGS,
            "Bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
            "Display" to Settings.ACTION_DISPLAY_SETTINGS,
            "Sound" to Settings.ACTION_SOUND_SETTINGS,
            "Location" to Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            "Apps" to Settings.ACTION_APPLICATION_SETTINGS,
            "Battery" to Settings.ACTION_BATTERY_SAVER_SETTINGS,
            "Storage" to Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            "Security" to Settings.ACTION_SECURITY_SETTINGS,
            "Developer Options" to Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            "NFC" to Settings.ACTION_NFC_SETTINGS,
            "Network" to Settings.ACTION_NETWORK_OPERATOR_SETTINGS
        )
        
        val lowerQuery = query.lowercase()
        val results = settingsMap.filter { it.key.lowercase().contains(lowerQuery) }
            .map { (name, action) ->
                UniversalSearchResult(id = "set_$name", title = name, subtitle = "System Settings") {
                    try {
                        val intent = Intent(action)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {}
                }
            }
        
        if (results.isEmpty()) {
            return listOf(UniversalSearchResult(id = "set_empty", title = "No settings found") {})
        }
        return results
    }

    fun getContactsResults(activity: Activity, query: String): List<UniversalSearchResult> {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return listOf(UniversalSearchResult(id = "cont_perm", title = "Tap to grant Contacts permission") {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_CONTACTS), 101)
            })
        }

        if (query.isEmpty()) {
            return listOf(UniversalSearchResult(id = "cont_empty", title = "Type to search contacts") {})
        }

        val results = mutableListOf<UniversalSearchResult>()
        
        // Use Phone URI to get phone numbers directly for dialing
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID, 
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        
        try {
            val cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC")
            cursor?.use {
                while (it.moveToNext() && results.size < 15) {
                    val id = it.getString(0)
                    val name = it.getString(1) ?: "Unknown"
                    val number = it.getString(2) ?: ""
                    
                    results.add(UniversalSearchResult(
                        id = "cont_$id", 
                        title = name, 
                        subtitle = number,
                        endIconRes = android.R.drawable.ic_menu_info_details,
                        endAction = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try { activity.startActivity(intent) } catch (e: Exception) {}
                        }
                    ) {
                        if (number.isNotBlank()) {
                            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CALL_PHONE), 103)
                            } else {
                                val intent = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                activity.startActivity(intent)
                            }
                        }
                    })
                }
            }
        } catch (e: Exception) {}

        if (results.isEmpty()) {
            results.add(UniversalSearchResult(id = "cont_none", title = "No contacts found") {})
        }
        return results
    }

    fun getFilesResults(activity: Activity, query: String): List<UniversalSearchResult> {
        val hasExternal = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val hasImages = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        
        if (!hasExternal && !hasImages) {
            return listOf(UniversalSearchResult(id = "file_perm", title = "Tap to grant Storage permission") {
                val perms = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
                } else {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                ActivityCompat.requestPermissions(activity, perms, 102)
            })
        }

        if (query.isEmpty()) {
            return listOf(UniversalSearchResult(id = "file_empty", title = "Type to search files") {})
        }

        val results = mutableListOf<UniversalSearchResult>()
        
        // Add main folders manually
        val standardFolders = listOf(
            "Downloads" to android.os.Environment.DIRECTORY_DOWNLOADS,
            "Pictures" to android.os.Environment.DIRECTORY_PICTURES,
            "Movies" to android.os.Environment.DIRECTORY_MOVIES,
            "Music" to android.os.Environment.DIRECTORY_MUSIC,
            "Documents" to android.os.Environment.DIRECTORY_DOCUMENTS
        )
        
        for ((name, type) in standardFolders) {
            if (name.lowercase().contains(query.lowercase())) {
                results.add(UniversalSearchResult(id = "folder_$name", title = name, subtitle = "Folder") {
                    val intent = Intent(Intent.ACTION_VIEW)
                    val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:$type")
                    intent.setDataAndType(uri, "vnd.android.document/directory")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try { activity.startActivity(intent) } catch (e: Exception) {}
                })
            }
        }

        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.MIME_TYPE, MediaStore.Files.FileColumns.DATA)
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        
        try {
            val cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
            cursor?.use {
                while (it.moveToNext() && results.size < 15) {
                    val id = it.getString(0)
                    val name = it.getString(1) ?: "Unknown"
                    val mimeType = it.getString(2) ?: "*/*"
                    val data = it.getString(3) ?: ""
                    
                    results.add(UniversalSearchResult(id = "file_$id", title = name, subtitle = data) {
                        val intent = Intent(Intent.ACTION_VIEW)
                        val contentUri = ContentUris.withAppendedId(uri, id.toLong())
                        intent.setDataAndType(contentUri, mimeType)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        try { activity.startActivity(intent) } catch (e: Exception) {}
                    })
                }
            }
        } catch (e: Exception) {}

        if (results.isEmpty()) {
            results.add(UniversalSearchResult(id = "file_none", title = "No files found") {})
        }
        return results
    }
}