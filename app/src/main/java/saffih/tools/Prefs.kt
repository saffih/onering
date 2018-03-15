package saffih.tools


import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.AttributeSet


open class Prefs(appContext: Context) {
    private var created = false
    private var halted = false

    val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)
    /**
     * override and
     * PreferenceManager.setDefaultValues(appContext, R.xml.preferences, false)
     *
     */
    open fun defaultInit() {
        // PreferenceManager.setDefaultValues(appContext, R.xml.preferences, true);

    }

    val spChanged = SharedPreferences.OnSharedPreferenceChangeListener { pref, key ->
        this@Prefs.onSharedPreferenceChanged(pref, key)
    }

    open fun onSharedPreferenceChanged(pref: SharedPreferences?, key: String?) {
    }

    open fun onCreate() {
        created = true
        val key = "$sig"
        if (get(key, "").isEmpty()) {
            defaultInit()
            put(key, key)
        }
        preferences.registerOnSharedPreferenceChangeListener(spChanged)
    }

    open fun onDestroy() {
        halted = true
        preferences.unregisterOnSharedPreferenceChangeListener(spChanged)
    }

    fun get(key: String, default: String) = preferences.getString(key, default)
    fun getString(key: String) = get(key, "")
    fun get(key: String, default: Int) = preferences.getInt(key, default)
    fun getListString(key: String) = getString(key).split(sig)
    fun getStrings(key: String) = getStringsItems(key).unzip().second
    fun getStringsItems(key: String): List<Pair<String, String>> {
        val sizeKey = "${key}${sig}size"
        val size = get(sizeKey, 0)
        return (0..size - 1).map { keyOf(key, it) }.map { it to getString(it) }
    }

    fun putStrings(key: String, lst: List<String>) {
        put(key, lst.hashCode())
        val sizeKey = "${key}${sig}size"
        val size = get(sizeKey, 0)
        val newSize = lst.size

        lst.forEachIndexed { index, i -> put(keyOf(key, index), i) }
        (newSize..size).forEach { put(keyOf(key, it), "") }
        put(sizeKey, newSize)
    }


    fun get(key: String, default: Boolean) = preferences.getBoolean(key, default)
    fun getListBoolean(key: String) = getListString(key).map { (it == "true") }

    fun put(key: String, value: Int) = preferences.edit().putInt(key, value).apply()
    fun put(key: String, value: String) = preferences.edit().putString(key, value).apply()

    fun putListString(key: String, stringList: List<String>) {
        val myStringList = stringList.toTypedArray()
        val toSave = myStringList.joinToString(sig)
        preferences.edit().putString(key, toSave).apply()
    }

//    fun putBoolean(key: String, value: Boolean) {
//
//        preferences.edit().putBoolean(key, value).apply()
//    }

    fun putListBoolean(key: String, boolList: List<Boolean>) {
        val newList = boolList.map { if (it) "true" else "false" }
        putListString(key, newList)
    }

    fun remove(key: String) = preferences.edit().remove(key).apply()

    fun clear() = preferences.edit().clear().apply()

    val all: Map<String, *>
        get() = preferences.all

    companion object {
        fun keyOf(key: String, index: Int) = "${key}${sig}${index}"
        val sep = '\u204F'
        val sig = "${sep};${sep}"
    }
}


class EditTextShowPreference : android.preference.EditTextPreference {
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context) : super(context)

    override fun getSummary(): CharSequence {
        val summary1 = super.getSummary()
        val summary = summary1?.toString() ?: "%s"

        return String.format(summary, text)
    }
}