/*
 * By Saffi Hartal, 2017.
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

/*
 * By Saffi Hartal, 5/29/17 3:29 PM
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

/*
 * By Saffi Hartal, 2017
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

//// local service
//// 1. ElmEngine
//// 2. dispatchReply(replyMsg), onBind
//// 3. return an reference the elm impl
//// 4. local service delegate on bindState etc. ( return the binder implemented by the)
//// 5. client for connect - get ap
//
//
//
//package saffih.elmdroid.service
//
//import android.app.Service
//import android.content.ComponentName
//import android.content.Context
//import android.content.Intent
//import android.content.ServiceConnection
//import android.os.Binder
//import android.os.IBinder
//import saffih.elmdroid.ElmChild
//
///**
// * Copyright Joseph Hartal (Saffi)
// * Created by saffi on 14/05/17.
// */
//
////class StateLocalService<M, MSG, API : MSG>(
////        val me: Service, val debug:Boolean):
////        StateChild<M, MSG>() {
////
////
////    fun toast(txt: String, duration: Int = Toast.LENGTH_SHORT) {
////        if (!debug) return
////        val handler = Handler(Looper.getMainLooper())
////        handler.post({ Toast.makeText(me, txt, duration).show() })
////    }
////
////    protected fun dispatchReply(smsg: API) {
////    }
////
//////    private val handler = object : Handler() {
//////        override fun handleMessage(message: Message) {
//////            lastincomingMessage = message
//////            val smsg = toApi(message)
//////            if (smsg == null) {
//////                super.handleMessage(message)
//////            } else {
//////                // any reply would use dispatchReply to return the response.
//////                dispatch(smsg)
//////            }
//////        }
//////    }
////
////    /**
////     * When binding to the service, we return an interface to our messenger
////     * for sending messages to the service.
////     */
////    val self = this
////    inner class LocalBinder(): Binder() {
////        val service=self
////    }
////    fun onBind(intent: Intent): IBinder {
////       return LocalBinder()
////    }
//////
//////
//////
//////    fun onCreate() { // for long lived services
//////        start()
//////    }
//////
//////
//////    fun onDestroy() {}
////}
//
//
//abstract class LocalService : Service() {
//    /**
//     * Class for clients to access.  Because we know this service always
//     * runs in the same process as its clients, we don't need to deal with
//     * IPC.
//     */
//
//    override  fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
////        Log.i("LocalService", "Received start id $startId: $intent")
//        return START_NOT_STICKY
//    }
//
//    val self = this
//    inner class LocalBinder(): Binder() {
//        val service=self
//    }
//    override fun onBind(intent: Intent): IBinder {
//       return LocalBinder()
//    }
//}
//////fun <LOCALSERVICE:LocalService>LOCALSERVICE.onBind(intent: Intent): IBinder = LocalBinder(this)
////
////
//abstract class LocalServiceClient<LOCALSERVICE:LocalService, M, MSG>(val me: Context,
//                                                                     val javaClassName: Class<*>) : ElmChild<M, MSG>()  {
//    var bound:LOCALSERVICE?=null
//    abstract fun onConnected(): Unit
//    abstract fun onDisconnected(): Unit
//
//    val mConnection = object : ServiceConnection {
//        override fun onServiceConnected(className: ComponentName, service: IBinder) {
//            // This is called when the connection with the service has been
//            // established, giving us the object we can use to
//            // interact with the service.  We are communicating with the
//            // service using a Messenger, so here we get a client-side
//            // representation of that from the raw IBinder object.
//            val b = service as LocalService.LocalBinder
//            bound = b.service as LOCALSERVICE
//            onConnected()
//        }
//
//        override fun onServiceDisconnected(className: ComponentName) {
//            // This is called when the connection with the service has been
//            // unexpectedly disconnected -- that is, its process crashed.
//            onDisconnected()
//            bound = null
//        }
//    }
//
//    fun doUnbindService() {
//        if (bound!=null) {
//            // Detach our existing connection.
//            bound = null
//            me.unbindService(mConnection)
//        }
//    }
//}
////
////
////inline fun <reified LOCALSERVICE:LocalService, M, MSG>LocalServiceClient<LOCALSERVICE, M, MSG>.doBindService():Boolean {
////    // Establish a connection with the service.  We use an explicit
////    // class name because we want a specific service implementation that
////    // we know will be running in our own process (and thus won't be
////    // supporting component replacement by other applications).
////    return me.bindService(Intent(me,
////            LOCALSERVICE::class.java), mConnection, Context.BIND_AUTO_CREATE)
////}
////
////
//////inline fun <reified T : Activity> Activity.startActivity() {
//////    startActivity(Intent(this, T::class.java))
//////}
