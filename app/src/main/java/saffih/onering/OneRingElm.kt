/*
 * Copyright (c) 5/29/17 3:35 PM. by Saffi Hartal
 */

package saffih.onering

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import saffih.elmdroid.ElmBase
import saffih.elmdroid.Que
import saffih.elmdroid.service.client.ElmMessengerServiceClient
import saffih.elmdroid.service.client.MService
import saffih.onering.mylocation.LocationActivity
import saffih.onering.service.*
import saffih.onering.settings.SettingsActivity
import saffih.tools.TinyDB
import saffih.elmdroid.service.client.Model as ClientModel
import saffih.elmdroid.service.client.Msg as ClientMsg

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

    //    sealed class Child():Msg() {
//        data class MainService(val smsg: MainMsg) :Child()
//    }
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

class OneRingElm(override val me: AppCompatActivity) : ElmBase<Model, Msg>(me),
        NavigationView.OnNavigationItemSelectedListener {

    fun toast(txt: String, duration: Int = Toast.LENGTH_LONG) {
//        val handler = Handler(Looper.getMainLooper())
//        handler.
//                post({
        Toast.makeText(me, txt, duration).show()
//    })
    }

    inner class MainsElmRemoteServiceClient(me: Context) :
            ElmMessengerServiceClient<MainMsgApi>(me, javaClassName = MainService::class.java,
                    toApi = { it.toApi() },
                    toMessage = { it.toMessage() }) {

        override fun onAPI(msg: MainMsgApi) {
            when (msg) {
                is saffih.onering.service.Msg.Api.Request.ConfChange -> {
                    toast("${msg}")
                }
            }
        }

        override fun onConnected(msg: MService) {
            super.onConnected(msg)

        }


    }

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

    override fun init(): Pair<Model, Que<Msg>> {
        return ret(Model(), Msg.Init())
    }

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when (msg) {
            is Msg.Init -> {
                ret(model)
            }
//            is Msg.Child.MainService -> {
//                val (m,c) = update(msg, model.state)
//                ret(model.copy(state = m), c)
//            }
            is Msg.Activity -> {
                val (m, q) = update(msg, model.activity)
                ret(model.copy(activity = m), q)
            }
            is Msg.Step.Updated -> {
                ret(model.copy(state = msg.state))
            }
        }
    }

//    private fun  update(msg: Msg.Child.MainService, model: MState): Pair<MState, Que<Msg>> {
//
//        val (m,c) = mainServiceClient.update(msg.smsg, model)
//
//        return ret(model)
//    }

//    fun update(msg: Msg, model: MActivity): Pair<MActivity, Que<Msg>> {
//        return when (msg) {
//            is Msg.Init -> {
//                ret(model)
//            }
//            is Msg.Activity -> {
//                Snackbar.make(msg.view, "Exit", Snackbar.LENGTH_LONG)
//                        .setAction("Finish", { me.finish() }).show()
//                ret(model)
//            }
//            is Msg.Activity.Option -> {
//                val (m, q) = update(msg, model.options)
//                ret(model.copy(options = m), q)
//            }
//            is Msg.Activity.Action.OpenTwitter -> {
//                val name = msg.name
//                me.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/${name}")))
//                ret(model)
//            }
//            is Msg.Activity.Action.UIToast -> {
//                msg.show(me)
//                ret(model)
//            }
//            is Msg.Child.MainService -> TODO()
//        }
//    }

    fun update(msg: Msg.Activity, model: MActivity): Pair<MActivity, Que<Msg>> {
        return when (msg) {
            is Msg.Activity.Fab.Clicked -> {
                Snackbar.make(msg.view, "Exit", Snackbar.LENGTH_LONG)
                        .setAction("Finish", { me.finish() }).show()
                ret(model)
            }
            is Msg.Activity.Option -> {
                val (m, q) = update(msg, model.options)
                ret(model.copy(options = m), q)
            }
            is Msg.Activity.Action.OpenTwitter -> {
                val name = msg.name
                me.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/${name}")))
                ret(model)
            }
            is Msg.Activity.Action.UIToast -> {
                msg.show(me)
                ret(model)
            }
            is Msg.Activity.Action.ShowLocation -> {
                me.startActivity(
                        Intent(me, LocationActivity::class.java))
                ret(model)
            }
        }
    }

    fun update(msg: Msg.Activity.Option, model: MOptions): Pair<MOptions, Que<Msg>> {
        return when (msg) {
            is Msg.Activity.Option.ItemSelected -> {
                val (m, c) = update(msg, model.itemOption)
                ret(model.copy(itemOption = m), c)
            }
            is Msg.Activity.Option.Navigation -> {
                val (m, c) = update(msg, model.navOption)
                ret(model.copy(navOption = m), c)
            }
            is Msg.Activity.Option.Drawer -> {
                val (m, c) = update(msg, model.drawer)
                ret(model.copy(drawer = m), c)
            }
        }
    }

    private fun update(msg: Msg.Activity.Option.ItemSelected, model: MItemOption): Pair<MItemOption, Que<Msg>> {
//         return ret(tickets)
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
                ret(MItemOption(item = selected))
            }
            else -> ret(model.copy(handled = false))
        }
    }

    private fun update(msg: Msg.Activity.Option.Navigation, model: MNavOption): Pair<MNavOption, Que<Msg>> {
        //        return ret(tickets)
        val item = msg.item
        // Handle navigation view item clicks here.
        val id = item.itemId
        val nav = NavOption.byId(id)
        val close = Msg.Activity.Option.Drawer(DrawerOption.closed)
        return if (nav == null) {
            // dispatch(Msg.Option.Log(DrawerOption.closed))
            ret(model.copy(nav = null), close)
        } else {
            when (nav) {
                NavOption.Location -> {
                    me.startActivity(
                            Intent(me, LocationActivity::class.java))
                    ret(model, close)
                }
                NavOption.Send -> {
                    val allowedList = me.effectiveAllowedList()
                    if (!allowedList.isEmpty()) {
                        val sendTo = allowedList[0].phoneFormat()
                        toast(" Sending location to $sendTo")
                        mainServiceClient.request(MainMsgApi.dbgGot(sendTo, "1ring"))
                        if (TinyDB(me).getBoolean("openmap_switch")) {
                            me.startActivity(
                                    Intent(me, LocationActivity::class.java))
                        }

                        ret(model, close)//listOf(close, Msg.Activity.Action.ShowLocation()))
                    } else {
                        toast(" No filtering by calling number. Unsecured !!! please assign and enable in the settings.")
                        ret(model)
                    }
                }
                else -> ret(model)
            }

        }
    }

    fun update(msg: Msg.Activity.Option.Drawer, model: MDrawer): Pair<MDrawer, Que<Msg>> {
        return when (msg.item) {
            DrawerOption.opened -> ret(model.copy(i = DrawerOption.opened))
            DrawerOption.closed -> ret(model.copy(i = DrawerOption.closed))
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
            val toolbar = me.findViewById(R.id.toolbar) as Toolbar
            me.setSupportActionBar(toolbar)
        }
        checkView(setup, model, pre) {
            //view(., pre?. )
        }
    }

    private fun view(model: MFab, pre: MFab?) {
        val setup = {
            val fab = me.findViewById(R.id.fab) as FloatingActionButton
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
            val navigationView = me.findViewById(R.id.nav_view) as NavigationView
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
            val drawer = me.findViewById(R.id.drawer_layout) as DrawerLayout
            val navView = me.findViewById(R.id.nav_view) as NavigationView
            val parentLayout = navView.getHeaderView(0)
            val nameView = parentLayout.findViewById(R.id.creatorNameView) as TextView
            val name = me.resources.getString(R.string.twitter_account)
            nameView.text = "@" + name
            nameView.setOnClickListener { view -> dispatch(Msg.Activity.Action.OpenTwitter(name)) }

            val toolbar = me.findViewById(R.id.toolbar) as Toolbar

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
            val drawer = me.findViewById(R.id.drawer_layout) as DrawerLayout
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
