package com.tomdunkley.dailypuzzles.data.developer

import android.content.Context
import android.content.SharedPreferences

object DeveloperStore {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("developer_store", Context.MODE_PRIVATE)
    }

    var isDeveloper: Boolean
        get() = prefs.getBoolean("is_developer", false)
        set(value) { prefs.edit().putBoolean("is_developer", value).apply() }

    fun clear() {
        prefs.edit().remove("is_developer").apply()
    }
}
