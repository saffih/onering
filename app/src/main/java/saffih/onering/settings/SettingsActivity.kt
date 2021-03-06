/*
 * Copyright (c) 5/29/17 3:35 PM. by Saffi Hartal
 */

package saffih.onering.settings


import android.annotation.TargetApi
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import android.provider.ContactsContract
import android.support.v4.app.NavUtils
import android.view.MenuItem
import saffih.elmdroid.ElmMachine
import saffih.elmdroid.checkView
import saffih.elmdroid.service.LocalServiceClient
import saffih.onering.MainMsgApi
import saffih.onering.R
import saffih.onering.service.MainService
import saffih.onering.service.phoneFormat
import saffih.onering.settings.allowed.ElmPreferenceSettings
import saffih.tools.AppCompatPreferenceActivity
import saffih.tools.post
import saffih.onering.settings.allowed.Model as AllowedModel
import saffih.onering.settings.allowed.Msg as AllowedMsg


data class MMenuItemSelected(val featureId: Int, val item: MenuItem, val handled: Boolean)
data class MOptions(val menuItemSelected: MMenuItemSelected? = null)
data class Model(val options: MOptions = MOptions(),
                 val allowed: AllowedModel = AllowedModel()
)

sealed class Msg {
    sealed class Options : Msg() {
        class MenuItemSelected(val featureId: Int, val item: MenuItem) : Options()
        class UnhandledMenuItemSelected(val featureId: Int, val item: MenuItem) : Options()

    }

    sealed class Child : Msg() {
        data class Allowed(val smsg: AllowedMsg) : Child()
    }


    sealed class Init : Msg() {
        class Allowed(val fragment: Fragments.GeneralPreferenceFragment) : Init()

    }
}

enum class SettingsActionResult {
    PICK_ALLOWED_CONTACT_REQUEST// The request code
    ;
}


class AppSettings(val me: SettingsActivity) : ElmMachine<Model, Msg>() {
    inner class MainsElmRemoteServiceClient(me: Context) :
            LocalServiceClient<MainService>(me, localserviceJavaClass = MainService::class.java) {
        override fun onReceive(payload: Message?) {

        }

        fun updateForegroundNotification() {
            bound?.app?.dispatch(MainMsgApi.updateForegroundNotification())
        }
    }

    val mainServiceClient = MainsElmRemoteServiceClient(me)


    private val allowedApp = object : ElmPreferenceSettings(me) {
        override fun handleMSG(cur: saffih.onering.settings.allowed.Msg) {
            dispatch(Msg.Child.Allowed(cur))
        }
//        override fun allowChanged() {
//            mainServiceClient.request(MainMsgApi.settingsChange)
//        }

//        override fun postDispatch(edited: saffih.onering.settings.allowed.Msg.Allowed.Edited) {
//            postDispatch(Msg.Child.Allowed(edited))
//        }

    }

    override fun init() = Model()

    override fun update(msg: Msg, model: Model): Model {
        return when (msg) {
            is Msg.Options -> {
                val m = update(msg, model.options)
                model.copy(options = m)
            }
            is Msg.Init.Allowed -> {
                val m = allowedApp.update(AllowedMsg.init(msg.fragment), model.allowed)
                model.copy(allowed = m)

            }
            is Msg.Child.Allowed -> {
                val m = allowedApp.update(msg.smsg, model.allowed)
                model.copy(allowed = m)
            }
        }
    }

    private fun update(msg: Msg.Options, model: MOptions): MOptions {
        return when (msg) {
            is Msg.Options.MenuItemSelected -> model
            is Msg.Options.UnhandledMenuItemSelected -> {
                if (msg.item.itemId == android.R.id.home) {
                    NavUtils.navigateUpFromSameTask(me)
                }
                model
            }
        }
    }

    override fun view(model: Model, pre: Model?) {
        val setup = {}
        checkView(setup, model, pre) {
            allowedApp.view(model.allowed, pre?.allowed)
        }

    }

    fun gotActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val action = SettingsActionResult.values()[requestCode]
        // Check which request we're responding to
        when (action) {
            SettingsActionResult.PICK_ALLOWED_CONTACT_REQUEST -> {
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    // The user picked a contact.
                    // The Intent's data Uri identifies which contact was selected.

                    // Get the URI that points to the selected contact
                    val contactUri = data?.data
                    // We only need the NUMBER column, because there will be only one row in the result
                    val PHONE_NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER
                    val projection = arrayOf(PHONE_NUMBER)

                    // Perform the query on the contact to get the NUMBER column
                    // We don't need a selection or sort order (there's only one result for the given URI)
                    // CAUTION: The query() method should be called from a separate thread to avoid blocking
                    // your app's UI thread. (For simplicity of the sample, this code doesn't do that.)
                    // Consider using CursorLoader to perform the query.
                    val cursor = me.contentResolver
                            .query(contactUri, projection, null, null, null)
                    cursor!!.moveToFirst()

                    // Retrieve the phone number from the NUMBER column
                    val column = cursor.getColumnIndex(PHONE_NUMBER)
                    val number = cursor.getString(column).phoneFormat()
                    dispatch(Msg.Child.Allowed(AllowedMsg.add(number)))
                } else {
                    dispatch(Msg.Child.Allowed(AllowedMsg.add("type a number")))
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        mainServiceClient.onCreate()
    }

    override fun onDestroy() {
        mainServiceClient.onDestroy()
        super.onDestroy()
    }
}

/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 *
 * See [
   * Android Design: Settings](http://developer.android.com/design/patterns/settings.html) for design guidelines and the [Settings
   * API Guide](http://developer.android.com/guide/topics/ui/settings.html) for more information on developing a Settings UI.
 */
class SettingsActivity : AppCompatPreferenceActivity() {

    val app = AppSettings(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()
        app.onCreate()
    }

    override fun onDestroy() {
        app.onDestroy()
        super.onDestroy()
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        app.gotActivityResult(requestCode, resultCode, data)
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        app.dispatch(Msg.Options.MenuItemSelected(featureId, item))
        if (app.myModel.options.menuItemSelected?.handled == true)
            return true
        val res = super.onMenuItemSelected(featureId, item)
        if (!res) {
            app.dispatch(Msg.Options.UnhandledMenuItemSelected(featureId, item))
        }
        return res
    }

    /**
     * {@inheritDoc}
     */
    override fun onIsMultiPane(): Boolean {
        return isXLargeTablet(this)
    }

    /**
     * {@inheritDoc}
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
                || Fragments.GeneralPreferenceFragment::class.java.name == fragmentName
                || Fragments.OptionsPreferenceFragment::class.java.name == fragmentName
//                || Fragments.DataSyncPreferenceFragment::class.java.name == fragmentName
//                || Fragments.NotificationPreferenceFragment::class.java.name == fragmentName
    }


    companion object {

        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        private fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
        }
    }
}


class Fragments {

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class GeneralPreferenceFragment : PreferenceFragment() {

        // The request code
        // Show user only contacts w/ phone numbers
        fun pickContact() {
            val uri = ContactsContract.Contacts.CONTENT_URI
            val pickContactIntent = Intent(Intent.ACTION_PICK, uri)

            pickContactIntent.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE // Show user only contacts w/ phone numbers
            activity.startActivityForResult(pickContactIntent, SettingsActionResult.PICK_ALLOWED_CONTACT_REQUEST.ordinal)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val app = (activity as SettingsActivity).app
            addPreferencesFromResource(R.xml.pref_contacts)
            setHasOptionsMenu(true)
            val addContact = findPreference("add_contact")
            addContact.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                pickContact()
                true
            }

            val msg = Msg.Init.Allowed(this)
            app.dispatch(msg)
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class OptionsPreferenceFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val app = (activity as SettingsActivity).app
            addPreferencesFromResource(R.xml.pref_options)
            setHasOptionsMenu(true)

            val useForeground = findPreference("foreground_service_and_notification_switch")
            useForeground.onPreferenceChangeListener = object : Preference.OnPreferenceChangeListener {
                override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
                    activity.post {
                        app.mainServiceClient.updateForegroundNotification()
                    }
                    return true
                }
            }
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }
}