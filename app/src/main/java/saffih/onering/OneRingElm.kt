/*
 * Copyright (c) 5/29/17 3:35 PM. by Saffi Hartal
 */

package saffih.onering

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_one_ring.*
import kotlinx.android.synthetic.main.app_bar_one_ring.*
import saffih.elmdroid.ElmMachine
import saffih.elmdroid.checkView
import saffih.elmdroid.service.LocalServiceClient
import saffih.onering.mylocation.LocationActivity
import saffih.onering.service.MainService
import saffih.onering.service.MyPrefs
import saffih.onering.service.effectiveAllowedList
import saffih.onering.service.phoneFormat
import saffih.onering.settings.SettingsActivity

typealias MainMsg = saffih.onering.service.Msg
typealias MState = saffih.onering.service.MState
typealias MainMsgApi = saffih.onering.service.Msg.Api
typealias MainMsgApiReply = saffih.onering.service.Msg.Api.Reply


//import us.feras.mdv.MarkdownView


/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 29/04/17.
 */

// UI options
enum class NavOption(val id: Int) {
    Location(R.id.nav_location),
    //    Log(R.id.nav_log),
//    Filter(R.id.nav_filter),
//    Share(R.id.nav_share),
    Send(R.id.nav_send);

    companion object {
        val map by lazy { values().associate { it.id to it } }
        fun byId(id: Int) = map.get(id)
    }
}

enum class ItemOption(val id: Int) {
    settings(R.id.action_settings);

    companion object {
        val map by lazy { values().associate { it.id to it } }
        fun byId(id: Int) = map.get(id)
    }
}


sealed class Msg {
    class Init : Msg()
    sealed class Activity : Msg() {
        sealed class Fab : Activity() {
            class Clicked(val view: View) : Fab()
        }

        sealed class Option : Activity() {
            class Navigation(val item: MenuItem) : Option()
            class ItemSelected(val item: MenuItem) : Option()
            class Drawer(val item: DrawerOption = DrawerOption.opened) : Option()
        }

        sealed class Action : Activity() {
            class OpenTwitter(val name: String) : Action()
            class UIToast(val txt: String, val duration: Int = Toast.LENGTH_SHORT) : Action()
            class ShowLocation : Action()
        }
    }

    sealed class Step : Msg() {
        class Updated(val state: MState) : Step()

    }
}

fun Msg.Activity.Action.UIToast.show(me: Context) {
    val toast = Toast.makeText(me, txt, duration)
    toast.show()
}

data class Model(
        val activity: MActivity = MActivity(),
        val state: MState = MState())

data class MActivity(
        val pager: MViewPager = MViewPager(),
        val fab: MFab = MFab(),
        val toolbar: MToolbar = MToolbar(),
        val options: MOptions = MOptions()
)

data class MOptions(val drawer: MDrawer = MDrawer(),
                    val navOption: MNavOption = MNavOption(),
                    val itemOption: MItemOption = MItemOption())

data class MViewPager(val i: Int = 0)
data class MToolbar(val i: Int = 0)
data class MFab(val snackbar: MSnackbar = MSnackbar())
data class MSnackbar(val i: Int = 0)

data class MDrawer(val i: DrawerOption = DrawerOption.closed)
data class MNavOption(val toDisplay: Boolean = true, val nav: NavOption? = null)
data class MItemOption(val handled: Boolean = true, val item: ItemOption? = null)

sealed class DrawerOption {
    object opened : DrawerOption()
    object closed : DrawerOption()
}

class OneRingElm(val me: OneRingActivity) : ElmMachine<Model, Msg>(),
        NavigationView.OnNavigationItemSelectedListener {

    fun toast(txt: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(me, txt, duration).show()
    }

    inner class MainsElmRemoteServiceClient(me: Context) :
            LocalServiceClient<MainService>(me, localserviceJavaClass = MainService::class.java) {

        override fun onReceive(payload: Message?) {
            val msg = payload?.obj as MainMsgApi
            when (msg) {
                is saffih.onering.service.Msg.Api.Request.ConfChange -> {
                    toast("${msg}")
                }
            }
        }

        fun request(dbgGot: saffih.onering.service.Msg.Api.Request.Inject) {
            bound?.app?.dispatch(dbgGot)
        }
    }

    val prefs by lazy { MyPrefs(me) }
    val mainServiceClient = MainsElmRemoteServiceClient(me)

/*
*
* */


    override fun onCreate() {
        super.onCreate()
        mainServiceClient.onCreateUnbound()
    }


    override fun onDestroy() {
        mainServiceClient.onDestroy()
        super.onDestroy()
    }

    override fun init(): Model {
        dispatch(Msg.Init())
        return Model()
    }

    override fun update(msg: Msg, model: Model): Model {
        return when (msg) {
            is Msg.Init -> {
                model
            }
//            is Msg.Child.MainService -> {
//                val (m,c) = update(msg, model.state)
//                model.copy(state = m)
//            }
            is Msg.Activity -> {
                val m = update(msg, model.activity)
                model.copy(activity = m)
            }
            is Msg.Step.Updated -> {
                model.copy(state = msg.state)
            }
        }
    }

//    private fun  update(msg: Msg.Child.MainService, model: MState) : MState {
//
//        val (m,c) = mainServiceClient.update(msg.smsg, model)
//
//        return model)
//    }

//    fun update(msg: Msg, model: MActivity) : MActivity {
//        return when (msg) {
//            is Msg.Init -> {
//                model)
//            }
//            is Msg.Activity -> {
//                Snackbar.make(msg.view, "Exit", Snackbar.LENGTH_LONG)
//                        .setAction("Finish", { me.finish() }).show()
//                model)
//            }
//            is Msg.Activity.Option -> {
//                val m = update(msg, model.options)
//                model.copy(options = m)
//            }
//            is Msg.Activity.Action.OpenTwitter -> {
//                val name = msg.name
//                me.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/${name}")))
//                model)
//            }
//            is Msg.Activity.Action.UIToast -> {
//                msg.show(me)
//                model)
//            }
//            is Msg.Child.MainService -> TODO()
//        }
//    }

    fun update(msg: Msg.Activity, model: MActivity): MActivity {
        return when (msg) {
            is Msg.Activity.Fab.Clicked -> {
                Snackbar.make(msg.view, "Exit", Snackbar.LENGTH_LONG)
                        .setAction("Finish", { me.finish() }).show()
                model
            }
            is Msg.Activity.Option -> {
                val m = update(msg, model.options)
                model.copy(options = m)
            }
            is Msg.Activity.Action.OpenTwitter -> {
                val name = msg.name
                me.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/${name}")))
                model
            }
            is Msg.Activity.Action.UIToast -> {
                msg.show(me)
                model
            }
            is Msg.Activity.Action.ShowLocation -> {
                me.startActivity(
                        Intent(me, LocationActivity::class.java))
                model
            }
        }
    }

    fun update(msg: Msg.Activity.Option, model: MOptions): MOptions {
        return when (msg) {
            is Msg.Activity.Option.ItemSelected -> {
                val m = update(msg, model.itemOption)
                model.copy(itemOption = m)
            }
            is Msg.Activity.Option.Navigation -> {
                val m = update(msg, model.navOption)
                model.copy(navOption = m)
            }
            is Msg.Activity.Option.Drawer -> {
                val m = update(msg, model.drawer)
                model.copy(drawer = m)
            }
        }
    }

    private fun update(msg: Msg.Activity.Option.ItemSelected, model: MItemOption): MItemOption {
//         return tickets)
        val item = msg.item
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        val selected = ItemOption.byId(id)
        return when (selected) {
            ItemOption.settings -> {
                me.startActivity(
                        Intent(me, SettingsActivity::class.java))
                MItemOption(item = selected)
            }
            else -> model.copy(handled = false)
        }
    }

    private fun update(msg: Msg.Activity.Option.Navigation, model: MNavOption): MNavOption {
        //        return tickets)
        val item = msg.item
        // Handle navigation view item clicks here.
        val id = item.itemId
        val nav = NavOption.byId(id)
        val close = Msg.Activity.Option.Drawer(DrawerOption.closed)
        return if (nav == null) {
            // dispatch(Msg.Option.Log(DrawerOption.closed))
            dispatch(close)
            model.copy(nav = null)
        } else {
            when (nav) {
                NavOption.Location -> {
                    dispatch(close)
                    me.startActivity(
                            Intent(me, LocationActivity::class.java))
                    model
                }
                NavOption.Send -> {
                    val allowedList = prefs.effectiveAllowedList()
                    if (!allowedList.isEmpty()) {
                        val sendTo = allowedList[0].phoneFormat()
                        toast(" Sending location to $sendTo")
                        mainServiceClient.request(MainMsgApi.dbgGot(sendTo, "1ring"))
                        if (prefs.get("openmap_switch", true)) {
                            me.startActivity(
                                    Intent(me, LocationActivity::class.java))
                        }
                        dispatch(close)
                        //listOf(close, Msg.Activity.Action.ShowLocation()))
                        model
                    } else {
                        toast(" No filtering by calling number. Unsecured !!! please assign and enable in the settings.")
                        model
                    }
                }
                else -> model
            }

        }
    }

    fun update(msg: Msg.Activity.Option.Drawer, model: MDrawer): MDrawer {
        return when (msg.item) {
            DrawerOption.opened -> model.copy(i = DrawerOption.opened)
            DrawerOption.closed -> model.copy(i = DrawerOption.closed)
        }
    }

    override fun view(model: Model, pre: Model?) {
        val setup = {
        }
        checkView(setup, model, pre) {
            view(model.activity, pre?.activity)
        }
    }

    private fun view(model: MActivity, pre: MActivity?) {
        val setup = {
            me.setContentView(R.layout.activity_one_ring)
        }
        checkView(setup, model, pre) {
            view(model.fab, pre?.fab)
            view(model.toolbar, pre?.toolbar)
            view(model.options, pre?.options)
        }
    }


    private fun view(model: MToolbar, pre: MToolbar?) {
        val setup = {
            val toolbar = me.toolbar
            me.setSupportActionBar(toolbar)
        }
        checkView(setup, model, pre) {
            //view(., pre?. )
        }
    }

    private fun view(model: MFab, pre: MFab?) {
        val setup = {
            val fab = me.fab
            fab.setOnClickListener { view -> dispatch(Msg.Activity.Fab.Clicked(view)) }
        }

        checkView(setup, model, pre) {
            view(model.snackbar, pre?.snackbar)
        }
    }

    private fun view(model: MSnackbar, pre: MSnackbar?) {
        val setup = {
        }
        checkView(setup, model, pre) {
            //view(tickets., pre?. )
        }
    }

    private fun view(model: MOptions, pre: MOptions?) {
        val setup = {
            val navigationView = me.nav_view
            navigationView.setNavigationItemSelectedListener(this)
        }
        checkView(setup, model, pre) {
            view(model.drawer, pre?.drawer)
            view(model.itemOption, pre?.itemOption)
            view(model.navOption, pre?.navOption)
        }
    }

    private fun view(model: MDrawer, pre: MDrawer?) {
        val setup = {
            val drawer = me.drawer_layout
            val navView = me.nav_view
            val parentLayout = navView.getHeaderView(0)

            val nameView = parentLayout.findViewById<TextView>(R.id.creatorNameView)
//            val nameView = me.creatorNameView // parentLayout.findViewById(R.id.creatorNameView) as TextView
            val name = me.resources.getString(R.string.twitter_account)
            nameView.text = "@" + name
            nameView.setOnClickListener { view -> dispatch(Msg.Activity.Action.OpenTwitter(name)) }

            val toolbar = me.toolbar

            val toggle = ActionBarDrawerToggle(
                    me, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
            drawer.addDrawerListener(toggle)
            val openListener = OpenedDrawerListener({
                dispatch(Msg.Activity.Option.Drawer(DrawerOption.opened))
            })
            val closeListener = ClosedDrawerListener({
                dispatch(Msg.Activity.Option.Drawer(DrawerOption.closed))
            })
            closeListener.registerAt(drawer)
            openListener.registerAt(drawer)
            toggle.syncState()

        }

        checkView(setup, model, pre) {
            val drawer = me.drawer_layout
            when (model.i) {
                DrawerOption.opened -> drawer.openDrawer(GravityCompat.START)
                DrawerOption.closed -> drawer.closeDrawer(GravityCompat.START)
            }
        }
    }

    private fun view(model: MNavOption?, pre: MNavOption?) {
        checkView({}, model, pre) {
            //view(tickets., pre?.a )
        }
    }

    private fun view(model: MItemOption?, pre: MItemOption?) {
        checkView({}, model, pre) {
            //view(tickets., pre?.a )
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        dispatch(Msg.Activity.Option.Navigation(item))
        return this.myModel.activity.options.navOption.toDisplay
    }

    fun onResume() {

    }

    fun onPause() {
    }
}


/**
 * Nicer Listener API for the drawer
 */
open class BlankDrawerListener : DrawerLayout.DrawerListener {
    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
    }

    override fun onDrawerOpened(drawerView: View) {
    }

    override fun onDrawerClosed(drawerView: View) {
    }

    override fun onDrawerStateChanged(newState: Int) {
    }

    fun registerAt(drawerLayout: DrawerLayout) {
        drawerLayout.addDrawerListener(this)
    }

    operator fun invoke(drawerLayout: DrawerLayout) {
        registerAt(drawerLayout)
    }
}

open class OpenedDrawerListener(val f: (View) -> Unit) : BlankDrawerListener() {
    override fun onDrawerOpened(drawerView: View) {
        f(drawerView)
    }
}

open class ClosedDrawerListener(val f: (View) -> Unit) : BlankDrawerListener() {
    override fun onDrawerClosed(drawerView: View) {
        f(drawerView)
    }
}
