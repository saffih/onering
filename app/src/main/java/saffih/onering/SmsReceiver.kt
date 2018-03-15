/*
 * Copyright (c) 7/29/17 11:08 PM. by Saffi Hartal
 */

package saffih.onering

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import saffih.tools.post


open class SMSReceiverAdapter(val hook: (Array<out SmsMessage?>) -> Unit,
                              open val priority: Int? = null
) : BroadcastReceiver() {

    fun meRegister(me: Context) {
        val filter = IntentFilter()
        filter.priority = priority ?: filter.priority

        filter.addAction("android.provider.Telephony.SMS_RECEIVED")
        me.registerReceiver(this, filter)
    }

    fun meUnregister(me: Context) {
        me.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        context.post {
            hook(extractSms(intent))
        }
    }


    companion object {

        private fun constructSmsFromPDUs(rawPduData: Array<*>): Array<SmsMessage?> {
            val smsMessages = arrayOfNulls<SmsMessage>(rawPduData.size)
            for (n in rawPduData.indices) {
                smsMessages[n] = SmsMessage.createFromPdu(rawPduData[n] as ByteArray)
            }
            return smsMessages.filterNotNull().toTypedArray()
        }

        fun extractSms(intent: Intent): Array<out SmsMessage?> {
            val smsMessages = if (Build.VERSION.SDK_INT >= 19) { //KITKAT
                Telephony.Sms.Intents.getMessagesFromIntent(intent)
            } else {
                constructSmsFromPDUs(intent.extras?.get("pdus") as Array<*>)
            }
            return smsMessages
        }
    }
}

