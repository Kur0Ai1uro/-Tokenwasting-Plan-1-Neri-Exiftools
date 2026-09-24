package com.neri.exiftools.data

import android.content.Context
import com.neri.exiftools.util.SaveLocation
import com.neri.exiftools.util.SavePathRules

class SaveLocationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): SaveLocation {
        val relative = SavePathRules.normalize(prefs.getString(KEY_RELATIVE, null).orEmpty())
            ?: SavePathRules.DEFAULT
        val tree = prefs.getString(KEY_TREE, null)?.takeIf { it.isNotBlank() }
        return SaveLocation(relativePath = relative, treeUri = tree)
    }

    fun saveRelative(path: String) {
        prefs.edit()
            .putString(KEY_RELATIVE, path)
            .remove(KEY_TREE)
            .apply()
    }

    fun saveTree(uri: String) {
        prefs.edit().putString(KEY_TREE, uri).apply()
    }

    fun reset() {
        prefs.edit()
            .putString(KEY_RELATIVE, SavePathRules.DEFAULT)
            .remove(KEY_TREE)
            .apply()
    }

    private companion object {
        const val PREFS = "neri_save_location"
        const val KEY_RELATIVE = "relative_path"
        const val KEY_TREE = "tree_uri"
    }
}
