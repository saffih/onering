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

package saffih.elmdroid

/**
 *
 * Copyright Joseph Hartal (Saffi)  23/04/17.
 */


import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log

// default looper if none provided
val mainLooper by lazy { Looper.getMainLooper() }

abstract class MsgQue<MSG>(looper: Looper?, val what: Int) : Handler(looper) {
    constructor(what: Int) : this(mainLooper, what)
    constructor() : this(mainLooper, 0)

    override fun handleMessage(msg: Message?) {
        val cur = msg?.obj as MSG
        handleMSG(cur)
    }

    abstract fun handleMSG(cur: MSG)
    open fun dispatch(msg: MSG) {
        val m = Message.obtain(null, what, msg)
        sendMessage(m)
    }
}

/**
 * Abstract state machine - implemented by StateChild as a part of a larger one,
 * or as a StateMachine with it's own eventloop,
 */
interface StatePattern<M, MSG> {

    val que: MsgQue<MSG>
    // Update helper - for Iterable a. delegate updates b. chain the commands in que
    fun <M, MSG, SMSG : MSG> update(msg: SMSG,
                                    iterable: Iterable<M>,
                                    updateElement: (SMSG, M) -> M

    ): List<M> {
        return iterable.map({ updateElement(msg, it) })
    }


    fun dispatch(msg: MSG) = que.dispatch(msg)
    fun dispatch(pending: List<MSG>) = pending.map { dispatch(it) }


    // Mandatory methods
    // Elm Init - init : (Model, Que Msg)
    fun init(): M

    // In Elm - update : Msg -> Model -> (Model, Que Msg)
    fun update(msg: MSG, model: M): M


    fun onCreate()
    fun onDestroy()
}

/**
 * Interface for extending a StateMachine to an Elm pattern - having a view update
 * when all events were processed.
 */
interface Viewable<M> {
    fun view(model: M, pre: M?)
}

/**
 * Macro like method used by view implementation.
 */
fun <TM> checkView(setup: () -> Unit, model: TM, pre: TM?, render: () -> Unit) {
    if (model === pre) return
    if (pre === null) {
        setup()
    }
    render()
}

// Elm Pattern
interface ElmPattern<M, MSG> : StatePattern<M, MSG>, Viewable<M>

/**
 * StateChild uses all the parent resources, eventloop, state which is given and returnned.
 * The only child owned part is the que that is used for holding messages.
 * Process of the messages by wraping with a parent message and delegate to the parent.
 * the call for the init/update/view is done directly by the parent - unwrapping the it's message
 * and provide the child it's message inside.
 */
abstract class StateChild<M, MSG> : StatePattern<M, MSG> {
    override val que = object : MsgQue<MSG>() {
        override fun handleMSG(cur: MSG) {
            this@StateChild.handleMSG(cur)
        }
    }

    override fun onCreate() {
    }

    override fun onDestroy() {
    }

    abstract fun handleMSG(cur: MSG)
}

abstract class ElmChild<M, MSG> : StateChild<M, MSG>(), Viewable<M>

//abstract class ElmChildAdapter<M, MSG>(val delegate: ElmPattern<M, MSG>) : ElmChild<M, MSG>() {
//    override fun onCreate() = delegate.onCreate()
//    override fun onDestroy() = delegate.onDestroy()
//
//    override fun init() = delegate.init()
//    override fun update(msg: MSG, model: M) = delegate.update(msg, model)
//    override fun view(model: M, pre: M?) = delegate.view(model, pre)
//}


/**
 * Statemachine which advance by dispatching of message MSG
 * replacing the state M
 */
abstract class StateMachine<M, MSG> : StatePattern<M, MSG> {
    private val TAG: String = StateMachine::class.java.name
    var cnt = 0
    open val what: Int = 1
    open val looper: Looper get () = mainLooper
    override val que: MsgQue<MSG> by lazy {
        object : MsgQue<MSG>(this@StateMachine.looper, this@StateMachine.what) {
            override fun handleMSG(cur: MSG) {
                this@StateMachine.handleMSG(cur)
            }
        }
    }

    /**
     * inner loop processing messages
     */
    fun handleMSG(cur: MSG) {
        if (halted) return
        cnt += 1

        val modelUpdated = update(cur, myModel)
        onHandledMsg(cur, myModel, modelUpdated)
        _model = modelUpdated
        if (cnt > 1000) throw RuntimeException("Do we have a loop $cnt, last msg was $cur")
        if (!hasMessages()) {
            cnt = 0
            onQueIsEmpty()
        }
    }

    /**
     * Debug code
     */
    protected open fun onHandledMsg(cur: MSG, myModel: M, modelUpdated: M) {
        val s = "Msg: $cur \n Model: $myModel \n ===> $modelUpdated"
        Log.d(TAG, s)
    }

    open fun hasMessages(): Boolean {
        return que.hasMessages(what)
    }

    open fun onQueIsEmpty() {
    }

    // implementation vars - the latest state reference.
    private var _model: M? = null

    fun notStarted() = _model === null
    override fun onCreate() {

        if (notStarted()) {
            start()
            onStarted()
        }
    }

    open fun onStarted() {}

    var halted = false
    override fun onDestroy() {
        que.post {
            _model = null
            halted = true
        }
    }

    // expose our immutable model as myModel
    val myModel: M
        get () {
            return _model!!
        }


    fun start() {
        // must
        que.post {
            assert(_model == null) { "Check if started more then once." }
            _model = init()
            onInitDone()
        }
    }

    protected open fun onInitDone() {
        // on ElmMachine we update the view after the init.
    }
}

abstract class ElmMachine<M, MSG> : StateMachine<M, MSG>(), Viewable<M> {

    override fun onInitDone() {
        val model = myModel
        callView(model)
    }

    private var model_viewed: M? = null

    //In Elm - view : Model -> Html Msg
    open fun view(model: M) {
        view(model, model_viewed)
    }

    private fun callView(model: M) {
        view(model)
        model_viewed = model
    }

    override fun onQueIsEmpty() {
        // update the view
        callView(myModel)
    }
}
