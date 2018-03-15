/*
 * Copyright (c) 5/29/17 3:35 PM. by Saffi Hartal
 */

package saffih.onering

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import saffih.tools.toast

class OneRingActivity : AppCompatActivity() {
    val app = OneRingElm(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
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
        val perms: List<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            perms = listOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
            ActivityCompat.requestPermissions(this,
                    perms.toTypedArray(), PermCode.NOTIFICATION.ordinal)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isNotificationPolicyAccessGranted()) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }


    enum class PermCode {
        ZERO,
        ALL,
        NOTIFICATION;

        companion object {
            fun fromId(id: Int): PermCode? {
                if (id <= 0) return null
                if (id >= values().size) return null
                return values().get(id)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (PermCode.fromId(requestCode)) {
            OneRingActivity.PermCode.ZERO -> {
                //never
            }
            OneRingActivity.PermCode.ALL -> {
                onALLRequestPermissionsResult(permissions, grantResults)
            }
            OneRingActivity.PermCode.NOTIFICATION -> {
                onNotificationRequestPermissionsResult(permissions, grantResults)
            }
            null -> {
            }
        }
    }

    val requiredPerms = listOf(Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    )

    private fun onALLRequestPermissionsResult(permissions: Array<out String>, grantResults: IntArray) {
        val res = grantResults.zip(permissions).filter { it.first != PackageManager.PERMISSION_GRANTED }
        if (res.isNotEmpty()) {
            toast("Lack Permissions: " + res.map { it.second }.joinToString())
        }
    }


    private fun onNotificationRequestPermissionsResult(permissions: Array<out String>, grantResults: IntArray) {
        val res = grantResults.zip(permissions).filter { it.first != PackageManager.PERMISSION_GRANTED }
        if (res.isNotEmpty()) {
            toast("Lack Permissions: " + res.map { it.second }.joinToString())
        }
    }


    fun checkPermission() {
        val me = this
        ActivityCompat.requestPermissions(this, requiredPerms.toTypedArray(), PermCode.ALL.ordinal)
        me.requestNotificationPolicyAccess()
    }

    companion object
}
