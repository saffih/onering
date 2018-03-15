/*
 * Copyright (c) 5/29/17 3:35 PM. by Saffi Hartal
 */

package saffih.onering.mylocation

//import saffih.onering.gps.toApi

// get the extension methods
import android.location.Location
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import saffih.elmdroid.ElmMachine
import saffih.elmdroid.checkView
import saffih.elmdroid.gps.child.LocationRegisterHelper
import saffih.onering.R
import saffih.tools.post

//typealias GpsMsgApi = saffih.elmdroid.child.Msg.Api

class LocationActivity : FragmentActivity() {

    val app = LocationApp(this)
    // in this activity we wanted a narrow life cycle of start stop
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.onCreate()
        val location: Location? = savedInstanceState?.getParcelable("location")
        if (location != null) {
            app.dispatch(Msg.Activity.GotLocation(location))
        }
    }

}

sealed class Msg {
    object Init : Msg()

    sealed class Activity : Msg() {
        sealed class Map : Activity() {
            class Ready(val googleMap: GoogleMap) : Map()
            class AddMarker(val markerOptions: MarkerOptions) : Map()
            class MoveCamera(val cameraUpdate: CameraUpdate) : Map()
        }

        //        class FirstRequest : Activity()
        class GotLocation(val location: Location) : Activity()

    }
}


/**
 * Model representing the state of the system
 * All Model types are Prefixed with M
 */
data class Model(val activity: MActivity = MActivity())

data class MActivity(val mMap: MMap = MMap())
data class MMap(val googleMap: GoogleMap? = null,
                val markers: Set<MarkerOptions> = setOf<MarkerOptions>(),
                val camera: CameraUpdate? = null)

class LocationApp(val me: LocationActivity) : ElmMachine<Model, Msg>(), OnMapReadyCallback {
    val locationBind = object : LocationRegisterHelper(me) {
        override fun onLocationChanged(loc: Location) {
            dispatch(Msg.Activity.GotLocation(loc))
            unregister()
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationBind.register()
    }

    override fun onDestroy() {
        locationBind.unregister()
        super.onDestroy()
    }

    override fun init(): Model {
        dispatch(Msg.Init)
        return Model()
    }

    override fun update(msg: Msg, model: Model): Model {
        return when (msg) {
            is Msg.Init -> {
                model
            }
            is Msg.Activity -> {
                val activityModel = update(msg, model.activity)
                model.copy(activity = activityModel)
            }
        }
    }

    fun update(msg: Msg.Activity, model: MActivity): MActivity {
        return when (msg) {
            is Msg.Activity.Map -> {
                val mapModel = update(msg, model.mMap)
                model.copy(mMap = mapModel)
            }
            is Msg.Activity.GotLocation -> {
                val m = update(msg, model.mMap)
                model.copy(mMap = m)
            }
        }
    }

    private fun update(msg: Msg.Activity.GotLocation, model: MMap): MMap {
        val here = LatLng(msg.location.latitude, msg.location.longitude)
        val marker = MarkerOptions().position(here).title("you are here")
        val moveCam = CameraUpdateFactory.newLatLngZoom(here, 15.0f)
        return model.copy(markers = model.markers + marker, camera = moveCam)
    }

    fun update(msg: Msg.Activity.Map, model: MMap): MMap {
        return when (msg) {

            is Msg.Activity.Map.Ready -> {
                model.copy(googleMap = msg.googleMap)
            }
            is Msg.Activity.Map.AddMarker -> {
                model.copy(markers = model.markers + msg.markerOptions)

            }
            is Msg.Activity.Map.MoveCamera -> {
                model.copy(camera = msg.cameraUpdate)
            }
        }
    }

    override fun view(model: Model, pre: Model?) {
        checkView({}, model, pre) {
            view(model.activity, pre?.activity)
        }
    }

    private fun view(model: MActivity, pre: MActivity?) {
        val setup = {
            me.setContentView(R.layout.activity_location)
        }
        checkView(setup, model, pre) {
            view(model.mMap, pre?.mMap)
        }
    }

    private fun view(model: MMap, pre: MMap?) {
        val setup = {
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            val mapFragment = me.supportFragmentManager
                    .findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
        checkView(setup, model, pre) {
            val ui = model.googleMap?.uiSettings
            if (ui != null) {
                ui.isCompassEnabled = true
                ui.isZoomControlsEnabled = true
                ui.isMyLocationButtonEnabled = true
                ui.setAllGesturesEnabled(true)
            }
            checkView({}, model.markers, pre?.markers) {
                me.post {
                    model.markers.forEach {
                        model.googleMap?.addMarker(it)
                    }
                }
            }
            checkView({}, model.camera, pre?.camera) {
                me.post {
                    model.googleMap?.moveCamera(model.camera)
                }
            }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        dispatch(Msg.Activity.Map.Ready(googleMap))

    }
}


