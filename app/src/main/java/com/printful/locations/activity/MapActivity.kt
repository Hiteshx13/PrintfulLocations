package com.printful.locations.activity

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.printful.locations.R
import com.printful.locations.`interface`.LatLngInterpolator
import com.printful.locations.adapter.InfoWindowAdapter
import com.printful.locations.databinding.ActivityMapBinding
import com.printful.locations.model.UserModel
import com.printful.locations.utils.*
import com.printful.locations.utils.TcpClient.OnMessageReceived


var mTcpClient: TcpClient? = null
lateinit var infoAdapter: InfoWindowAdapter

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        fun getIntent(context: Context, email: String): Intent {
            var intent = Intent(context, MapActivity::class.java)
            intent.putExtra(PARAM_EMAIL, email)
            return intent
        }
    }

    private var googleMap: GoogleMap? = null
    private var email = ""
    private lateinit var binding: ActivityMapBinding
    var mapMarkers = HashMap<String?, Marker?>()
    var isServerConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_map)
        email = intent.getStringExtra(PARAM_EMAIL) ?: ""
        infoAdapter = InfoWindowAdapter(this@MapActivity)
    }

    override fun onStart() {
        super.onStart()
        if (isNetworkConnected(this)) {
            startServer()
        } else {
            binding.progressbar.visibility = View.GONE
            Toast.makeText(
                this,
                resources.getString(R.string.check_internet),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun startServer() {
        isServerConnected = true
        AsyncConnectServer(this@MapActivity)
            .execute("")
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this@MapActivity)
    }


    override fun onResume() {
        super.onResume()

        if (isNetworkConnected(this)) {
            if (!isServerConnected) {
                startServer()
            }
        } else {
            binding.progressbar.visibility = View.GONE
            Toast.makeText(
                this,
                resources.getString(R.string.check_internet),
                Toast.LENGTH_SHORT
            ).show()
        }

    }


    override fun onBackPressed() {
        super.onBackPressed()
        isServerConnected = false
        mTcpClient?.stopClient()
    }

    class AsyncConnectServer(var activity: MapActivity) : AsyncTask<String, String, TcpClient>() {
        override fun doInBackground(vararg message: String?): TcpClient? {

            //we create a TCPClient object
            mTcpClient =
                TcpClient(object :
                    OnMessageReceived {
                    //here the messageReceived method is implemented
                    override fun messageReceived(message: String?) {
                        //this method calls the onProgressUpdate
                        publishProgress(message)
                    }
                })
            mTcpClient!!.run()

            return null
        }

        override fun onProgressUpdate(vararg values: String) {
            super.onProgressUpdate(*values)
            activity.updateUI(values[0])
        }
    }

    fun updateUI(sResponse: String) {

        Log.d("TCP", "response: $sResponse")
        binding.progressbar.visibility = View.GONE

        if (sResponse.startsWith(USERLIST, true)) {
            googleMap?.clear()
            var response = sResponse.replace(USERLIST, "", true)
            var listUsers: List<String>? = null
            val listLocation: List<String> = response.split(";")
            for (i in listLocation.indices) {
                if (listLocation[i].trim().isNotEmpty()) {

                    listUsers = listLocation[i].split(",")

                    addMarker(
                        UserModel(
                            listUsers[0], //Id
                            listUsers[1], //name
                            listUsers[2], //profile
                            listUsers[3], //lat
                            listUsers[4] //lan
                        )
                    )
                }
            }

            /** zoom on first marker**/
            if (!listUsers.isNullOrEmpty()) {
                var zoomLatLan = LatLng(
                    listUsers[3].toDouble(),
                    listUsers[4].toDouble()
                )
                val cameraPosition =
                    CameraPosition.Builder().target(zoomLatLan).zoom(14.0f).build()
                val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
                googleMap?.moveCamera(cameraUpdate)
            }

        } else if (sResponse.startsWith(UPDATE, true)) {
            updateMarkerLocation(sResponse)
        }
    }

    /** Add markers on map**/
    fun addMarker(model: UserModel) {

        val markerOpt = MarkerOptions().position(
            LatLng(
                model.lat.toDouble(),
                model.lan.toDouble()
            )
        ).snippet(model.profile)


        var marker = googleMap?.addMarker(markerOpt)
        marker?.tag = model

        mapMarkers.put(model.id.trim(), marker)
        googleMap?.moveCamera(
            CameraUpdateFactory.newLatLng(
                LatLng(
                    java.lang.Double.valueOf(model.lat),
                    java.lang.Double.valueOf(model.lan)
                )
            )
        )
    }

    /** update marker locations by tcp response**/
    fun updateMarkerLocation(str: String) {
        val listUsers: List<String> = str.replace(UPDATE, "", true).split(",")
        var markerKey: String = listUsers[0].trim()
        var marker = mapMarkers.get(markerKey)!!
        animateMarker(
            marker, LatLng(
                listUsers[1].toDouble(),
                listUsers[2].toDouble()
            ), LatLngInterpolator.Spherical()
        )
    }


    override fun onMapReady(map: GoogleMap?) {
        this.googleMap = map
        googleMap?.setInfoWindowAdapter(infoAdapter)
        mTcpClient?.sendMessage("$AUTHORIZE $email")
    }
}