/*
 * Copyright (c) 5/29/17 3:35 PM. by Saffi Hartal
 */

package saffih.onering.settings.allowed


import android.preference.Preference
import android.preference.PreferenceFragment
import saffih.elmdroid.ElmChild
import saffih.elmdroid.checkView
import saffih.onering.R
import saffih.onering.service.MyPrefs
import saffih.onering.service.phoneFormat
import saffih.onering.service.updateAllowedList
import saffih.onering.settings.SettingsActivity
import saffih.tools.EditTextPrefBuilder
import saffih.tools.post


data class MAllowed(val lst: List<Pair<String, String>> = listOf())

data class Model(
        val fragment: PreferenceFragment? = null
        , val allowed: MAllowed = MAllowed())

sealed class Msg {
    companion object {
        fun add(number: String) = Allowed.Add(number)
        fun init(fragment: PreferenceFragment) = Init(fragment)
    }

    data class Init(val fragment: PreferenceFragment) : Msg()
    sealed class Allowed : Msg() {
        class Edited(val key: String) : Allowed()
        class Add(val number: String) : Allowed()
    }
}


abstract class ElmPreferenceSettings(val me: SettingsActivity) : ElmChild<Model, Msg>() {
    override fun init(): Model = Model()
    internal val prefs: MyPrefs  by lazy { object : MyPrefs(me) {} }

    private lateinit var last: Model
    private val mymodel get() = last
    override fun update(msg: Msg, model: Model): Model {
        last = when (msg) {
            is Msg.Init -> {
                // read
                val lst = prefs.updateAllowedList()
                model.copy(fragment = msg.fragment,
                        allowed = MAllowed(lst = lst))
            }

            is Msg.Allowed -> {
                val m = update(msg, model.allowed)
                model.copy(allowed = m)
            }
        }
        return last
    }

    private fun update(msg: Msg.Allowed, model: MAllowed): MAllowed {
        val m = when (msg) {
            is Msg.Allowed.Edited ->
                prefs.updateAllowedList()
            is Msg.Allowed.Add ->
                prefs.updateAllowedList(msg.number)
        }
        return model.copy(lst = m)
    }

    override fun view(model: Model, pre: Model?) {
        val fragment = model.fragment ?: return
        val setup = {
            //            updateAllowedList().forEach {  }
//            allowed_list_items().forEach { updateScreen(it) }


        }
        checkView(setup, model, pre) {
            val pre_size = pre?.allowed?.lst?.size ?: 0
            val size = model.allowed.lst.size
            val iM = maxOf(size, pre_size)
            (0..iM - 1).forEach {
                val m = model.allowed.lst.getOrNull(it)
                val other = pre?.allowed?.lst?.getOrNull(it)
                view(m, other)
            }
            if (size != pre_size) {
                val screen = fragment.preferenceScreen
                val item_in_order = model.allowed.lst.map { fragment.findPreference(it.first) }
                item_in_order.forEach { screen.removePreference(it) }
                item_in_order.forEach { screen.addPreference(it) }
//                item_in_order.first().icon =  a.getDrawable(R.styleable.IconPreferenceScreen_icon);

            }
        }
    }

    private fun view(model: Pair<String, String>?, pre: Pair<String, String>?) {
        val fragment = mymodel.fragment ?: return
        val setup = {
            val key = model!!.first
            val ep = EditTextPrefBuilder(me, R.xml.pref_allowed)
                    .setKey(key)
                    .setTitle("")
                    .setSummary(model.second).enabled()
                    .build()
            ep.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                preference.summary = newValue as String
                // since we use preference rather then the model as a single source of truth.
                // adjust later for dups/remove
                me.post { dispatch(Msg.Allowed.Edited(key)) }
                true
            }
            fragment.preferenceScreen.addPreference(ep)
            Unit
        }
        checkView(setup, model, pre) {
            if (model == null) {
                val dropped = fragment.findPreference(pre!!.first)
                fragment.preferenceScreen.removePreference(dropped)
            } else {
                val changed = fragment.findPreference(model.first)
                changed.summary = model.second.phoneFormat()
                changed.title = ""
            }
        }
    }
}
