package saffih.tools

import android.content.Context
import android.preference.EditTextPreference
import android.util.AttributeSet
import android.util.Xml
import org.xmlpull.v1.XmlPullParser


/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 21/05/17.
 */
class EditTextPrefBuilder(val context: Context, val resId: Int? = null) {

    private var title: String? = null
    private var summary: String? = null
    private var key: String? = null
    private var enabled: Boolean = false

    fun build(): EditTextPreference {
        val attrs = getAttributeSet(resId)

        val ep = EditTextPreference(context, attrs)
        if (key != null) ep.key = key
        ep.title = title
        ep.summary = summary
        ep.isEnabled = enabled
        return ep

    }

    private fun getAttributeSet(resId: Int?): AttributeSet? {
        if (resId != null) {
            val parser: XmlPullParser = context.resources.getXml(resId)
            while (true) {
                val e = parser.eventType
                when (e) {
                    XmlPullParser.START_TAG -> return Xml.asAttributeSet(parser)
                    XmlPullParser.END_DOCUMENT -> return null
                    else -> {
                    }

                }
                parser.next()
            }


        }
        return null
    }

    init {
        this.title = ""
        this.summary = ""
        this.enabled = false
    }

    fun setTitle(title: String): EditTextPrefBuilder {
        this.title = title
        return this
    }

    fun setSummary(summary: String): EditTextPrefBuilder {
        this.summary = summary
        return this
    }

    fun setEnabled(enabled: Boolean): EditTextPrefBuilder {
        this.enabled = enabled
        return this
    }

    fun enabled() = setEnabled(true)

    fun setKey(key: String): EditTextPrefBuilder {
        this.key = key
        return this
    }

}