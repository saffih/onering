/*
 * Copyright (c) 5/29/17 3:35 PM. by Saffi Hartal
 */

package saffih.onering.settings.allowed


import android.app.Activity
import android.preference.Preference
import android.preference.PreferenceFragment
import saffih.elmdroid.ElmChild
import saffih.elmdroid.Que
import saffih.onering.R
import saffih.onering.service.updateAllowedList
import saffih.tools.EditTextPrefBuilder


data class MAllowed(val lst: List<Pair<String, String>> = listOf()
)

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


abstract class ElmPreferenceSettings(val me: Activity) : ElmChild<Model, Msg>() {
    override fun init(): Pair<Model, Que<Msg>> = ret(Model())
    private lateinit var last: Pair<Model, Que<Msg>>
    private val mymodel get() = last.first
    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        last = when (msg) {
            is Msg.Init -> {
                // read
                val lst = me.updateAllowedList()
                ret(model.copy(fragment = msg.fragment,
                        allowed = MAllowed(lst = lst)))
            }

            is Msg.Allowed -> {
                val (m, c) = update(msg, model.allowed)
                ret(model.copy(allowed = m), c)
            }
        }
        return last
    }

    private fun update(msg: Msg.Allowed, model: MAllowed): Pair<MAllowed, Que<Msg>> {
        val m = when (msg) {
            is Msg.Allowed.Edited ->
                me.updateAllowedList()
            is Msg.Allowed.Add ->
                me.updateAllowedList(msg.number)
        }
        return ret(model.copy(lst = m))
    }

    override fun view(model: Model, pre: Model?) {
        val fragment = model.fragment ?: return
        val setup = {
            //            updateAllowedList().forEach {  }
//            allowed_list_items().forEach { updateScreen(it) }


        }
        checkView(setup, model, pre) {
            val iM = maxOf(model.allowed.lst.size, pre?.allowed?.lst?.size ?: 0)
            (0..iM - 1).forEach {
                val m = model.allowed.lst.getOrNull(it)
                val other = pre?.allowed?.lst?.getOrNull(it)
                view(m, other)
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
                postDispatch(Msg.Allowed.Edited(key))
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
                changed.summary = model.second
                changed.title = ""
            }
        }
    }

    abstract fun postDispatch(edited: Msg.Allowed.Edited)
}
