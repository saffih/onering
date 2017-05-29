/*
 * Copyright (c) 5/29/17 3:35 PM. by Saffi Hartal
 */

package saffih.onering.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.AudioManager
import android.os.Bundle
import android.os.Message
import android.telephony.SmsMessage
import saffih.elmdroid.Que
import saffih.elmdroid.bindState
import saffih.elmdroid.gps.child.GpsChild
import saffih.elmdroid.service.ElmMessengerService
import saffih.elmdroid.service.ElmMessengerService.Companion.startService
import saffih.elmdroid.sms.child.MSms
import saffih.elmdroid.sms.child.SmsChild
import saffih.onering.OneRingActivity
import saffih.onering.persist.AllowedHelper
import saffih.tools.TinyDB
import java.util.*


/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 9/05/17.
 */
// "imports"
typealias GpsMsg = saffih.elmdroid.gps.child.Msg
typealias GpsMsgApi = saffih.elmdroid.gps.child.Msg.Api
typealias GpsModel = saffih.elmdroid.gps.child.Model

typealias SmsMsg = saffih.elmdroid.sms.child.Msg
//typealias SmsMsgApi = saffih.elmdroid.sms.child.Msg.Api
typealias SmsModel = saffih.elmdroid.sms.child.Model
typealias MSms = saffih.elmdroid.sms.child.MSms



fun Context.effectiveAllowedList(): List<String> {
    val tinydb = TinyDB(this)
    if (!tinydb.getBoolean("use_whitelist")) return listOf()
    return getAllowedList()
}

fun Context.getAllowedList(): List<String> {
    val tinydb = TinyDB(this)
    val key = "allowed_list"
    return tinydb.getStrings(key).filter { it != "" }.toSet().toList()
}

fun Context.updateAllowedList(added: String = ""): List<Pair<String, String>> {
    val tinydb = TinyDB(this)
    val key = "allowed_list"
    val values = tinydb.getStrings(key)
    val shrinked = (values + added).filter { it != "" }.toSet().toList()
    tinydb.putStrings(key, shrinked)

    return tinydb.getStringsItems(key)
}

data class Model(
        val gps: GpsModel = GpsModel(),
        val sms: SmsModel = SmsModel(),
        val api: MApi = MApi(),
        val state: MState = MState()
)


data class MState(val tickets: MTickets = MTickets(),
                  val conf: MConf = MConf()
)

data class MConf(val allowFrom: MWhiteList = MWhiteList())

data class MWhiteList(val allowed: List<String> = listOf()) {
    val lookup by lazy { allowed.toSet() }
}

class MApi

enum class TicketStatus {
    new,
    opened,
    pending,
    responded,
    closed;
}

enum class TicketEscalate {
    ping,
    unmute,
    screem;

    fun next(): TicketEscalate {
        val i = ordinal + 1
        return if (i < values().size) values()[i] else this
    }
}

data class MTickets(val tickets: List<MTicket> = listOf(),
                    val lookup: Map<String, Int> = mapOf())

data class MTicket(val number: String,
                   val status: TicketStatus = TicketStatus.new,
                   val queryDate: Date? = null,
                   val escalate: TicketEscalate = TicketEscalate.ping
) {
    fun key() = number
    fun match(other: String): Boolean {
        return other == number
    }
}

data class MSmsMessage(val number: String, val body: String) {
    constructor (sms: SmsMessage) : this(sms.originatingAddress, sms.messageBody)
}


sealed class Msg {
    fun isReply() = this is Msg.Api.Reply

    object Init : Msg()
    sealed class Response : Msg()


    sealed class Step : Msg() {
        data class ConfChange(val conf: MConf) : Step()
        data class GotSms(val sms: MSmsMessage) : Step() {
            constructor(number: String, body: String) : this(MSmsMessage(number, body))
        }

        sealed class Ticket : Step() {
            data class Open(val number: String) : Ticket()
            data class Reply(val locationMessage: String) : Ticket()
//            data class Close(val ticket: MTicket) : Ticket()

        }

        class GotLocation(val location: Location) : Step()
    }

    sealed class Child : Msg() {
        data class Gps(val smsg: GpsMsg) : Child()
        data class Sms(val smsg: SmsMsg) : Child()
    }


    sealed class Api : Msg() {
        companion object {
            fun confChange(conf: MConf) = Request.ConfChange(conf)
            fun dbgGot(number: String, txt: String) = Request.Inject(Msg.Step.GotSms(number, txt))
        }

        sealed class Request : Api() {
            class ConfChange(val conf: MConf) : Request()
            class Inject(val msg: Msg) : Request()
        }

        sealed class Reply : Api() {
            class Updated(val model: Model) : Reply()


        }

    }


//    companion object{
//        fun gpsRequest() = Msg.Child.Gps(
//            saffih.elmdroid.gps.child.Msg.Api.Request.Location())
//    }
}

//data class MyParcelable(val data1: String, val data2: String) : DefaultParcelable {
//    override fun writeToParcel(dest: Parcel, flags: Int) { dest.write(data1, data2) }
//    companion object { @JvmField final val CREATOR = DefaultParcelable.generateCreator { MyParcelable(it.read(), it.read()) } }
//}

enum class API {
    RequestConfChange,
    StateChange,
    INJECT
}

fun Message.toApi(): Msg.Api {
    return when (this.what) {
        API.RequestConfChange.ordinal -> Msg.Api.confChange(this.obj as MConf)
        API.StateChange.ordinal -> Msg.Api.Reply.Updated(this.obj as Model)
        API.INJECT.ordinal -> Msg.Api.Request.Inject(this.obj as Msg)
        else -> {
            throw RuntimeException("${this} has no 'what' value set")
        }
    }
}

fun Msg.Api.toMessage(): Message {
    return when (this) {
        is Msg.Api.Reply.Updated -> Message.obtain(null, API.StateChange.ordinal, this.model)
        is Msg.Api.Request.ConfChange -> Message.obtain(null, API.RequestConfChange.ordinal, this.conf)
        is Msg.Api.Request.Inject -> Message.obtain(null, API.INJECT.ordinal, this.msg)
    }
}

class MainServiceElm(override val me: Service) : ElmMessengerService<Model, Msg, Msg.Api>(me,
        toApi = { it.toApi() },
        toMessage = { it.toMessage() },
        debug = true) {


    private val gps = bindState(object : GpsChild(me) {
//        override fun view(model: saffih.elmdroid.gps.child.Model, pre: saffih.elmdroid.gps.child.Model?) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }

        override fun onLocationChanged(location: Location) {
            postDispatch(Msg.Step.GotLocation(location))
        }
    }) { Msg.Child.Gps(it) }

    private val sms = bindState(object : SmsChild(me) {
        override fun onSmsArrived(sms: List<SmsMessage>) {
            addPending(sms.map { Msg.Step.GotSms(it.originatingAddress, it.messageBody) })
        }
    }) { Msg.Child.Sms(it) }


    override fun onDestroy() {
        gps.onDestroy()
        sms.onDestroy()
        super.onDestroy()

    }

    override fun onCreate() {
        super.onCreate()
        // the init initialize it.
//        gps.onCreate()
//        sms.onCreate()

    }

//    private val sms = bindState(object:SmsChild(me}{}) { Msg.Child.Sms(it) }

    fun settingChange(lst: List<String>): MConf {
        val conf = myModel.state.conf
        conf.copy(allowFrom = conf.allowFrom.copy(allowed = lst))
        return conf
    }

    override fun init(): Pair<Model, Que<Msg>> {
        val (gm, gc) = gps.init()
        val (sm, sc) = sms.init()
        val db = TinyDB(me)
        val lst = db.getStrings("allowed_list")
        db.preferences.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "allowed_list") {
                val lst2 = db.getStrings("allowed_list")
                dispatch(Msg.Step.ConfChange(settingChange(lst2)))
            }
        }
        val state = MState(conf = MConf(allowFrom = MWhiteList(lst)))
        return ret(Model().copy(gps = gm, sms = sm, state = state), Que(listOf(Msg.Init))
                + gc.map { Msg.Child.Gps(it) }
                + sc.map { Msg.Child.Sms(it) }
        )
    }

    val dbhelper = AllowedHelper(me)

    override fun update(msg: Msg, model: Model): Pair<Model, Que<Msg>> {
        return when (msg) {

            Msg.Init -> {
                sms.impl.onCreate()
//                ret(load(model))
                ret(model)
//                ret(model)
            }
            is Msg.Child.Gps -> {
                val (m, c) = gps.update(msg.smsg, model.gps)
                ret(model.copy(gps = m), c)
            }
            is Msg.Child.Sms -> {
                val (m, c) = sms.update(msg.smsg, model.sms)
                ret(model.copy(sms = m), c)
            }
            is Msg.Api -> {
                val (m, c) = update(msg, model.api)
                ret(model.copy(api = m), c)
            }
            is Msg.Step -> {
                val (m, c) = update(msg, model.state)
                ret(model.copy(state = m), c)
            }

        }
    }


    private fun numberPassedFilter(model: MState, number: String): Boolean {
        val endwith = number.takeLast(7)
        val whitelist = model.conf.allowFrom.allowed.
                map { it.replace("[-+ ]".toRegex(), "") }
                .map { it.takeLast(7) }
        val passed = whitelist.isEmpty() || whitelist.contains(endwith)
        return passed
    }


    private fun update(msg: Msg.Step, model: MState): Pair<MState, Que<Msg>> {
        return when (msg) {
            is Msg.Step.ConfChange -> {
                val m = model.copy(conf = msg.conf)
//                save(model)
                ret(m)
            }
            is Msg.Step.GotSms -> {
                val sms = msg.sms
                if (passedCheck(model, sms)) {
                    toast("got Reqest ${sms}")
                    ret(model, Msg.Step.Ticket.Open(sms.number))
                } else {
                    if (wordMatch(sms)) {
                        toast("ignored sms from ${sms.number}")
                    }
                    ret(model)
                }
            }
            is Msg.Step.Ticket -> {
                val (m, c) = update(msg, model.tickets)
                ret(model.copy(tickets = m), c)
            }
            is Msg.Step.GotLocation -> {
                val lat = msg.location.latitude
                val lon = msg.location.longitude
                val url = "http://maps.google.com/?q=${lat},${lon}"
                ret(model, Msg.Step.Ticket.Reply("current location: ${url}"))
            }
        }
    }

    private fun passedCheck(model: MState, sms: MSmsMessage): Boolean {
        val word = wordMatch(sms)
        return word && (numberPassedFilter(model, sms.number))
    }

    private fun wordMatch(sms: MSmsMessage): Boolean {
        val word = when (sms.body.trim().toLowerCase()) {
            "wru" -> true
            "1ring" -> true
            else -> {
                false
            }
        }
        return word
    }

    private fun update(msg: Msg.Step.Ticket, model: MTickets): Pair<MTickets, Que<Msg>> {
        val m = checkAddNewTicket(msg, model)

        val (tickets, c) = update(msg, m.tickets, this::updateTicket)
        return ret(model.copy(tickets = tickets, lookup = m.lookup), c)
    }

    private fun checkAddNewTicket(msg: Msg.Step.Ticket, model: MTickets): MTickets {
        return if (msg is Msg.Step.Ticket.Open) {
            val newTicket = MTicket(msg.number)
            // add ticket
            if (!model.lookup.containsKey(newTicket.key())) {
                updateTicket(msg, newTicket)
                val lst = model.tickets + newTicket
                val lookup = model.lookup.toMutableMap()
                lookup.put(newTicket.key(), lst.size)
                toast("ticket opened")
                model.copy(tickets = lst, lookup = lookup)
            } else {
                model
            }
        } else model
    }

    private fun unmute() {
        val audioManager = me.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        val zenModeValue = Settings.Global.getInt(me.getContentResolver(), "zen_mode")
        try {

            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        } catch(e: SecurityException) {
            toast("Please allow getting out of do not disturb")
        }
    }

    private fun maxVolume() {
        val audioManager = me.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)

        unmute()
        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, AudioManager.FLAG_SHOW_UI + AudioManager.FLAG_PLAY_SOUND)
    }

    private fun updateTicket(msg: Msg.Step.Ticket, model: MTicket): Pair<MTicket, Que<Msg>> {
        return when (msg) {

            is Msg.Step.Ticket.Open -> {
                if (!model.match(msg.number))
                    ret(model)
                else {
                    // check if already in progress - escalate.
                    val c = Msg.Child.Gps(GpsMsgApi.locate())
                    val now = Date()
                    val escalateFlag = if (model.queryDate != null) {
                        ((now.time - model.queryDate.time) / 1000 < 30)
                    } else false
                    val escalate = if (escalateFlag) model.escalate.next() else TicketEscalate.ping
                    when (escalate) {
                        TicketEscalate.ping -> {
                            startOneRing("ping")
                        }
                        TicketEscalate.unmute -> {
                            unmute()
                            toast("unmute")
                        }
                        TicketEscalate.screem -> {
                            maxVolume()
                            toast("Volume set to max")
                        }
                    }
                    ret(model.copy(
                            status = TicketStatus.opened,
                            queryDate = now,
                            escalate = escalate), c)
                }
            }

            is Msg.Step.Ticket.Reply -> {
                if (model.status != TicketStatus.opened) {
                    ret(model)
                } else {
                    // once we do processing inside then we would use a message.
                    sms.impl.sendSms(MSms(model.number, msg.locationMessage))
//                val c = Msg.Child.Sms(SmsMsgApi.sms(destinationAddress = model.number, text=msg.locationMessage))
                    ret(model.copy(status = TicketStatus.closed))
                    //, listOf( Msg.Step.Ticket.Close(model)) )
                }
            }
//            is Msg.Step.Ticket.Close -> {// removed
//                if (model==)
//                toast("ticket closed")
//                ret(model.copy(status = TicketStatus.closed))
//            }
        }
    }

    private fun startOneRing(action: String) {
        toast(action)
        val intent = Intent(me, OneRingActivity::class.java)
        val b = Bundle()
        b.putString("action", action) //Your id
        intent.putExtras(b)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        me.startActivity(intent)
    }


    /**
     * Communicate with Activity
     */
    private fun update(msg: Msg.Api, model: MApi): Pair<MApi, Que<Msg>> {
        return when (msg) {
            is Msg.Api.Request -> {
                when (msg) {
                    is Msg.Api.Request.ConfChange -> ret(model, Msg.Step.ConfChange(msg.conf))
                    is Msg.Api.Request.Inject -> {
                        ret(model, msg.msg)
                    }
                }
            }
            is Msg.Api.Reply -> {
                when (msg) {
                    is Msg.Api.Reply.Updated -> {
                        // ack - the parent already had dispatched with the replies
                        ret(model)
                    }

                }
            }
        }
    }


}


class BootCompletedIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceClass = MainService::class.java
        if ("android.intent.action.BOOT_COMPLETED" == intent.action) {
            val pushIntent = Intent(context, serviceClass)
            context.startService(pushIntent)
        }
    }
}


class OnSmsIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if ("android.provider.Telephony.SMS_RECEIVED" == intent.action) {
            startService(context, MainService::class.java)
//            val arr = extractSms(intent)
//            arr.filterNotNull().forEach {
//                startService(context, MainService::class.java//)
//                        , null, Msg.Step.GotSms(it.originatingAddress, it.messageBody))
//            }
        }
    }


}

class MainService : Service() {
    val app = MainServiceElm(this)


    override fun onBind(intent: android.content.Intent): android.os.IBinder {
        return app.onBind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        app.onCreate()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        app.onStartCommand(intent, flags, startId)
//        return super.onStartCommand(intent, flags, startId)
        return Service.START_REDELIVER_INTENT

    }

    override fun onDestroy() {
        app.onDestroy()
        super.onDestroy()
    }

    override fun onRebind(intent: Intent) {
        super.onRebind(intent)
        app.onRebind(intent)

    }

    override fun onUnbind(intent: Intent): Boolean {
        return app.onUnbind(intent)
    }


}