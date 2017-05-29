/*
 * Copyright (c) 5/29/17 3:35 PM. by Saffi Hartal
 */

package saffih.onering

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import saffih.elmdroid.activityCheckForPermission

class OneRingActivity : AppCompatActivity() {
    val app = OneRingElm(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission(4)
        app.onCreate()
    }

    override fun onResume() {
        super.onResume()
        app.onResume()
    }

    override fun onPause() {
        super.onPause()
        app.onPause()
    }

    override fun onBackPressed() {
        if (DrawerOption.opened == app.myModel.activity.options.drawer.i) {
            app.dispatch(Msg.Activity.Option.Drawer(DrawerOption.closed))
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.one_ring, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        app.dispatch(Msg.Activity.Option.ItemSelected(item))
        if (app.myModel.activity.options.itemOption.handled) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun isNotificationPolicyAccessGranted(): Boolean {
        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun requestNotificationPolicyAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isNotificationPolicyAccessGranted()) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }

    fun checkPermission(code: Int = 1) {
        val me = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val lst = listOf(Manifest.permission.RECEIVE_BOOT_COMPLETED,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_NOTIFICATION_POLICY
            )
            activityCheckForPermission(lst, code)
//            me.requestPermissions(lst.toTypedArray(), code)
        }
//        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED)
//        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//        ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
//        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
//        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY)
//        activityCheckForPermission(me, Manifest.permission.RECEIVE_BOOT_COMPLETED, code)
//            activityCheckForPermission(me, "android.permission.WRITE_EXTERNAL_STORAGE", 1)
//        SMSReceiverAdapter.checkPermission(me, code)
//        LocationAdapter.checkPermission(me, code)
        me.requestNotificationPolicyAccess()
//        activityCheckForPermission(me, "android.permission.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED", code)
    }

    companion object
}
