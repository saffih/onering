/*
 * Copyright (c) 5/29/17 3:35 PM. by Saffi Hartal
 */

package saffih.onering.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.telephony.SmsManager
import android.telephony.SmsMessage
import saffih.elmdroid.StateMachine
import saffih.elmdroid.gps.child.LocationRegisterHelper
import saffih.elmdroid.service.ElmMessengerService.Companion.startServiceIfNotRunning
import saffih.elmdroid.service.LocalService
import saffih.onering.OneRingActivity
import saffih.onering.R
import saffih.onering.SMSReceiverAdapter
import saffih.onering.mylocation.LocationActivity
import saffih.tools.Prefs
import saffih.tools.toast
import java.util.*


/**
 * Copyright Joseph Hartal (Saffi)
 * Created by saffi on 9/05/17.
 */


open class MyPrefs(private val context: Context) : Prefs(context) {
    override fun defaultInit() {
        super.defaultInit()
        PreferenceManager.setDefaultValues(context, R.xml.pref_options, true)
    }
}


fun MyPrefs.effectiveAllowedList(): List<String> {
    if (!get("use_whitelist", false)) return listOf()
    return getAllowedList()
}

fun MyPrefs.getAllowedList(): List<String> {
    val key = "allowed_list"
    return getStrings(key).filter { it != "" }.toSet().toList()
}

fun MyPrefs.updateAllowedList(added: String = ""): List<Pair<String, String>> {
    val key = "allowed_list"
    val values = getStrings(key)
    val shrinked = (values + added).filter { it != "" }.toSet().toList()
    putStrings(key, shrinked)

    return getStringsItems(key)
}

data class Model(
        val api: MApi = MApi(),
        val state: MState = MState()
)

data class MState(val tickets: MTickets = MTickets(),
                  val conf: MConf = MConf()
)

data class MConf(val allowFrom: MWhiteList = MWhiteList())

data class MWhiteList(val allowed: List<String> = listOf())

fun MConf.numberPassedFilter(number: String): Boolean {
    val endwith = number.cleanPhoneNumber().takeLast(7)
    val whitelist = this.allowFrom.allowed.map { it.cleanPhoneNumber().takeLast(7) }
    val passed = whitelist.isEmpty() || whitelist.contains(endwith)
    return passed
}


private fun wordMatch(txt: String): Boolean {
    val word = when (txt.trim().toLowerCase()) {
        "wru" -> true
        "1ring" -> true
        else -> {
            false
        }
    }
    return word
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

data class MSms(val address: String, val text: String)

data class MSmsMessage(val number: String, val body: String, val center: String) {
    constructor (sms: SmsMessage) : this(sms.originatingAddress, sms.messageBody, sms.serviceCenterAddress ?: "")
}


sealed class Msg {
    fun isReply() = this is Msg.Api.Reply

    object Init : Msg()

    sealed class Step : Msg() {
        data class ConfChange(val conf: MConf) : Step()
        data class GotSms(val sms: MSmsMessage) : Step() {
            constructor(number: String, body: String, center: String = "") : this(MSmsMessage(number, body, center))
        }

        sealed class Ticket : Step() {
            data class Open(val sms: MSmsMessage) : Ticket()
            data class Reply(val locationMessage: String) : Ticket()

        }

        class GotLocation(val location: Location) : Step()
    }
//
//    sealed class Child : Msg() {
////        data class Gps(val smsg: GpsMsg) : Child()
//        data class Sms(val smsg: SmsMsg) : Child()
//    }


    sealed class Api : Msg() {
        companion object {
            fun updateForegroundNotification() = Request.UpdateForegroundNotification()
            fun confChange(conf: MConf) = Request.ConfChange(conf)
            fun dbgGot(number: String, txt: String) = Request.Inject(Msg.Step.GotSms(number, txt))
        }

        sealed class Request : Api() {
            class ConfChange(val conf: MConf) : Request()
            class Inject(val msg: Msg) : Request()
            class UpdateForegroundNotification : Request()
        }

        sealed class Reply : Api() {
            class Updated(val model: Model) : Reply()


        }

    }
}


private fun Context.unmute() {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    try {

        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
    } catch (e: SecurityException) {
        toast("Please allow getting out of do not disturb")
    }
}

private fun Context.maxVolume() {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)

    unmute()
    audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, AudioManager.FLAG_SHOW_UI + AudioManager.FLAG_PLAY_SOUND)
}

//enum class API {
//    RequestConfChange,
//    StateChange,
//    INJECT, UpdateForegroundNotification
//}

//fun Message.toApi(): Msg.Api {
//    return when (this.what) {
//        API.RequestConfChange.ordinal -> Msg.Api.confChange(this.obj as MConf)
//        API.StateChange.ordinal -> Msg.Api.Reply.Updated(this.obj as Model)
//        API.INJECT.ordinal -> Msg.Api.Request.Inject(this.obj as Msg)
//        API.UpdateForegroundNotification.ordinal -> Msg.Api.Request.UpdateForegroundNotification()
//        else -> {
//            throw RuntimeException("${this} has no 'what' value set")
//        }
//    }
//}

class MainServiceElm(val me: Service) : StateMachine<Model, Msg>() {

    fun onSmsArrived(sms: List<SmsMessage>) {
        val pending = sms.map { Msg.Step.GotSms(MSmsMessage(it)) }
        dispatch(pending)
    }

    val prefs by lazy { MyPrefs(me) }

//    override fun onCreate() {
//        super.onCreate()
//    }

    override fun onDestroy() {
        locationBind.unregister()
        smsReceiver.meUnregister(me)
        super.onDestroy()

    }


    override fun init(): Model {
        val lst = prefs.getStrings("allowed_list")
        bindPrefListener()
        bindSmsListener()

        val state = MState(conf = MConf(allowFrom = MWhiteList(lst)))
        dispatch(Msg.Init)
        return Model().copy(
                state = state)
    }


    override fun update(msg: Msg, model: Model): Model {
        return when (msg) {

            Msg.Init -> {
                model
            }
            is Msg.Api -> {
                val m = update(msg, model.api)
                model.copy(api = m)
            }
            is Msg.Step -> {
                val m = update(msg, model.state)
                model.copy(state = m)
            }

        }
    }


//    private fun numberPassedFilter(model: MState, number: String): Boolean {
//        val endwith = number.cleanPhoneNumber().takeLast(7)
//        val whitelist = model.conf.allowFrom.allowed.
//                map { it.cleanPhoneNumber().takeLast(7) }
//        val passed = whitelist.isEmpty() || whitelist.contains(endwith)
//        return passed
//    }
//    val telephonyManager get () = me.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager


    private fun update(msg: Msg.Step, model: MState): MState {
        return when (msg) {
            is Msg.Step.ConfChange -> {
                val m = model.copy(conf = msg.conf)
                m
            }
            is Msg.Step.GotSms -> {
                val sms = msg.sms
                    if (passedCheck(model, sms)) {
                        toast("got Request ${sms}")
                        dispatch(Msg.Step.Ticket.Open(sms))
                        model
                    } else {
                        if (wordMatch(sms.body)) {
                            toast("Ignored sms from ${sms.number}")
                        }
                        model
                    }
            }
            is Msg.Step.Ticket -> {
                val m = update(msg, model.tickets)
                model.copy(tickets = m)
            }
            is Msg.Step.GotLocation -> {
                if (prefs.get("openmap_switch", true)) {
                    startShowLocation(msg.location)
                }
                val replyText = buildReplyText(msg)
                dispatch(Msg.Step.Ticket.Reply(replyText))
                model
            }
        }
    }

    private fun buildReplyText(msg: Msg.Step.GotLocation): String {
        val lat = msg.location.latitude
        val lon = msg.location.longitude
        val url = "http://maps.google.com/?q=${lat},${lon}"
        val replyText = "current location: ${url}"
        return replyText
    }


    private fun passedCheck(model: MState, sms: MSmsMessage): Boolean {
//        make_effort_to_detect_spoofed_sms_switch
        val word = wordMatch(sms.body)
        return word && (model.conf.numberPassedFilter(sms.number))
    }


    private fun update(msg: Msg.Step.Ticket, model: MTickets): MTickets {
        val m = checkAddNewTicket(msg, model)

        val tickets = update(msg, m.tickets, this::updateTicket)
        return model.copy(tickets = tickets, lookup = m.lookup)
    }

    private fun checkAddNewTicket(msg: Msg.Step.Ticket, model: MTickets): MTickets {
        return if (msg is Msg.Step.Ticket.Open) {
            val newTicket = MTicket(msg.sms.number)
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


    private fun updateTicket(msg: Msg.Step.Ticket, model: MTicket): MTicket {
        return when (msg) {

            is Msg.Step.Ticket.Open -> {
                if (!model.match(msg.sms.number))
                    model
                else {
                    // check if already in progress - escalate.
                    locationBind.register()
                    val queryDate = model.queryDate
                    val now = Date()
                    val escalateFlag = within30Sec(queryDate, now)
                    val escalate = if (escalateFlag) model.escalate.next() else TicketEscalate.ping
                    val pong = if (spoofedDetected(msg.sms)) {  // spoofedDetected can't escalte and response sent to real number
                        startOneRing("ping")
                        "Suspect spoofedDetected ... ${msg.sms}"
                    } else when (escalate) {
                        TicketEscalate.ping -> {
                            startOneRing("ping")
                            "Searching..."
                        }
                        TicketEscalate.unmute -> {
                            me.unmute()
                            val txt = "Unmuted"
                            toast(txt)
                            txt
                        }
                        TicketEscalate.screem -> {
                            me.maxVolume()
                            val txt = "Volume set to max"
                            toast(txt)
                            txt
                        }
                    }
                    if (Prefs(me).get("reply_each_request_with_status_switch", true)) {
                        sendSms(MSms(model.number, pong))
                    }
                    model.copy(
                            status = TicketStatus.opened,
                            queryDate = now,
                            escalate = escalate)
                }
            }

            is Msg.Step.Ticket.Reply -> {
                if (model.status != TicketStatus.opened) {
                    model
                } else {
                    // once we do processing inside then we would use a message.
                    sendSms(MSms(model.number, msg.locationMessage))
                    model.copy(status = TicketStatus.closed)
                }
            }
        }
    }

    private fun within30Sec(queryDate: Date?, now: Date): Boolean {
        return if (
                (queryDate != null)) {
            (Math.abs(now.time - queryDate.time) / 1000 < 30)
        } else false
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

    private fun startShowLocation(location: Location) {
        val intent = Intent(me, LocationActivity::class.java)
        val b = Bundle()
        intent.putExtras(b)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        b.putParcelable("location", location)
        me.startActivity(intent)
    }


    /**
     * Communicate with Activity
     */
    private fun update(msg: Msg.Api, model: MApi): MApi {
        return when (msg) {
            is Msg.Api.Request -> {
                when (msg) {
                    is Msg.Api.Request.ConfChange -> {
                        dispatch(Msg.Step.ConfChange(msg.conf))
                        model
                    }
                    is Msg.Api.Request.Inject -> {
                        dispatch(msg.msg)
                        model
                    }
                    is Msg.Api.Request.UpdateForegroundNotification -> {
                        (me as MainService).updateForegroundState()
                        model
                    }
                }
            }

            is Msg.Api.Reply -> {
                when (msg) {
                    is Msg.Api.Reply.Updated -> {
                        // ack - the parent already had dispatched with the replies
                        model
                    }
                }
            }
        }
    }


    private fun spoofedDetected(sms: MSmsMessage): Boolean {
        try {
            if (prefs.get("make_effort_to_detect_spoofed_sms_switch", false)) {
                val prefix = prefs.get("operator_prefix", "")
//                val yourNumber = telephonyManager.line1Number
//                if (telephonyManager.isNetworkRoaming) {
//                    if (yourNumber.length > 4) {
//                        val prefix = yourNumber.slice(0..4)
                if (!sms.center.startsWith(prefix)) {
                    toast(" suspect spoofing! got sms from center ${sms.center}. expected prefix ${prefix}")
                    return true
                }
//                    }
//                }
            }
        } catch (e: RuntimeException) {
            toast("Bug checking service center." + e)
        }
        return false
    }

    private fun bindSmsListener() {
        smsReceiver.meRegister(me)
    }

    /**
     * location listener that unregisters itself when done
     */
    val locationBind = object : LocationRegisterHelper(me) {
        override fun onLocationChanged(loc: Location) {
            dispatch(Msg.Step.GotLocation(loc))
            unregister()
        }
    }

    /**
     * sms listener  for checking every incomming sms
     */
    val smsReceiver = SMSReceiverAdapter(
            hook = { arr: Array<out SmsMessage?> -> onSmsArrived(arr.filterNotNull()) })

    val smsManager = SmsManager.getDefault()
    /**
     * sms send method for replying
     */
    fun sendSms(data: MSms) {
        smsManager.sendTextMessage(
                data.address,
                null,
                data.text, null, null)
    }

    private fun bindPrefListener() {
        prefs.preferences.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "allowed_list") {
                val lst2 = prefs.getStrings("allowed_list")
                dispatch(Msg.Step.ConfChange(settingChangePayloadBy(lst2)))
            }
        }
    }

    fun settingChangePayloadBy(lst: List<String>): MConf {
        val conf = myModel.state.conf
        conf.copy(allowFrom = conf.allowFrom.copy(allowed = lst))
        return conf
    }

    val debug = true
    /**
     * toast in debug
     */
    fun toast(txt: String) {
        if (!debug) return
        me.toast(txt)
    }


}


private fun startServiceIfNotRunning(context: Context) {
    startServiceIfNotRunning(context, MainService::class.java) {
        //        No need - default
//            it.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
//        Not broadcasting but recieving
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                it.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
//            }
//        }
    }
}


//class MainService : Service() {
class MainService : LocalService() {
    val app = MainServiceElm(this)

    fun updateForegroundState() {
        val shouldBe = app.prefs.get("foreground_service_and_notification_switch", true)
        updateForegroundState(shouldBe)
    }

    fun updateForegroundState(state: Boolean) {
        if (state) {
            runAsForeground()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(true)
                val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(R.id.notification_background)
            }
        }
    }

    private val channel1ring: String = "1ring channel"

    // not used
    private fun runAsForeground() {
//        val notificationManager =
//                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationIntent = Intent(this, OneRingActivity::class.java)

        val flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        notificationIntent.flags = flags

        val intent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification = NotificationCompat.Builder(this, channel1ring)
                .setSmallIcon(R.drawable.ic_1ring_notification)
                .setContentText(getString(R.string.app_name))
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build()


//        notification.setLatestEventInfo(context, title, message, intent);
//        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL;
//        notificationManager.notify(0, notification);


        startForeground(R.id.notification_background, notification)


        /*
        *
        *
            internal var notification = Notification(icon, message, `when`)
        *
        * */

    }


    override fun onCreate() {
        super.onCreate()
        app.onCreate()
        if (app.prefs.get("foreground_service_and_notification_switch", true)) {
            updateForegroundState(true)
        }


    }

//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        app.onStartCommand(intent, flags, startId)
//        return Service.START_REDELIVER_INTENT
//
//    }


    override fun onDestroy() {
        if (app.prefs.get("foreground_service_and_notification_switch", true)) {
            updateForegroundState(false)
        }
        app.onDestroy()

        super.onDestroy()
    }
}


fun String.phoneFormat(): String {
    return this.onlyDigits()?.toPhoneFormat() ?: this
}


fun String.onlyDigits(): String? {
    val res = cleanPhoneNumber()
    return if (res.all { it in ('0'..'9') }) res else null
}

fun String.cleanPhoneNumber() = this.replace("[-+() .]".toRegex(), "")

fun String.toPhoneFormat(): String? {
    return when (this.length) {
        7 -> "%s%s".format(substring(0, 3), substring(3, 7))
        10 -> "%s-%s%s".format(substring(0, 3), substring(3, 6), substring(6, 10))
        11 -> "%s(%s)%s%s".format(substring(0, 1), substring(1, 4), substring(4, 7), substring(7, 11))
        12 -> "+%s(%s)%s%s".format(substring(0, 3), substring(3, 5), substring(5, 8), substring(8, 12))
        else -> return null
    }
}


class BootCompletedIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if ("android.intent.action.BOOT_COMPLETED" == intent.action) {
            startServiceIfNotRunning(context)
        }
    }
}


class OnSmsIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if ("android.provider.Telephony.SMS_RECEIVED" == intent.action) {
            startServiceIfNotRunning(context)
        }
    }


}