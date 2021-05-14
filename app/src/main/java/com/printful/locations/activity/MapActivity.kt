package com.printful.locations.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.MeasureSpec
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.printful.locations.R
import com.printful.locations.`interface`.LatLngInterpolator
import com.printful.locations.databinding.ActivityMapBinding
import com.printful.locations.model.UserModel
import com.printful.locations.utils.*
import com.printful.locations.utils.TcpClient.OnMessageReceived
import kotlinx.android.synthetic.main.row_info_window.view.*

var mTcpClient: TcpClient? = null

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
    private var listLocation: List<String>? = null
    private lateinit var userList: HashMap<String, UserModel>
    var isImageLoading = false
    var imageLoadingID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_map)
        email = intent.getStringExtra(PARAM_EMAIL) ?: ""
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
            listLocation = response.split(";")
            userList = HashMap()
            for (i in listLocation!!.indices) {
                if (listLocation!![i].trim().isNotEmpty()) {

                    listUsers = listLocation!![i].split(",")
                    var model = UserModel(
                        listUsers[0], //Id
                        listUsers[1], //name
                        listUsers[2], //profile
                        listUsers[3], //lat
                        listUsers[4] //lan
                        , null, false
                    )
                    userList.put(listUsers[0].trim(), model)

                    addMarker(
                        model, false
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
                    CameraPosition.Builder().target(zoomLatLan).zoom(16.0f).build()
                val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
                googleMap?.moveCamera(cameraUpdate)
            }

        } else if (sResponse.startsWith(UPDATE, true)) {
            updateMarkerLocation(sResponse)
        }
    }

    /** Add markers on map**/
    fun addMarker(model: UserModel, isUpdateing: Boolean) {
        var newLat = model.lat
        var newLan = model.lan
        var marker: Marker? = null
        val view = this.layoutInflater.inflate(R.layout.row_info_window, null, false)
        if (isUpdateing) {
            marker = mapMarkers.get(model.id.trim())
            var uDataOld: UserModel? = userList[model.id.trim()]
            model.profile = uDataOld?.profile ?: ""
            model.name = uDataOld?.name ?: ""
            model.isImageLoaded = uDataOld?.isImageLoaded ?: false
            if (uDataOld?.image != null) {
                view.ivProfile.setImageDrawable(uDataOld.image)
            }
        }


        view.tvName.text = model.name
        view.tvAddress.text = getAddressFromLatLan(this, model.lat, model.lan).replace(",", "\n")


        view.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val markerOpt = MarkerOptions().position(
            LatLng(
                model.lat.toDouble(),
                model.lan.toDouble()
            )
        ).snippet(model.profile)
            .icon(BitmapDescriptorFactory.fromBitmap(loadBitmapFromView(view)))


        if (!isImageLoading && !model.isImageLoaded && imageLoadingID != model.id) {
            isImageLoading = true
            imageLoadingID = model.id
            Glide
                .with(this)
                .load(model.profile)
                .placeholder(R.drawable.ic_user)
                .listener(object : RequestListener<Drawable> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        data: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        view.ivProfile.setImageDrawable(resource)
                        marker?.setIcon(BitmapDescriptorFactory.fromBitmap(loadBitmapFromView(view)))

                        /**Updating marker with user image **/

                        var tempUserModel: UserModel? = userList[model.id.trim()] as UserModel
                        tempUserModel?.image = resource
                        tempUserModel?.isImageLoaded = true
                        userList.remove(model.id.trim())
                        userList.put(model.id.trim(), tempUserModel!!)

//                        userList.get(model.id.trim())?.isImageLoaded = true
                        isImageLoading = false
                        imageLoadingID = ""


                        if (!isUpdateing) {
                            marker?.setIcon(
                                BitmapDescriptorFactory.fromBitmap(
                                    loadBitmapFromView(
                                        view
                                    )
                                )
                            )
                        } else {
                            Log.d("update_2_", "" + newLat.toDouble() + "__" + newLan.toDouble())
                        }

                        return true
                    }
                })
                .into(view.ivProfile)


        }


        if (!isUpdateing) {

            marker = googleMap?.addMarker(markerOpt)
            marker?.tag = model
            mapMarkers.put(model.id.trim(), marker)

            Log.d("#Adding Marker", "" + marker?.id?.trim())
            googleMap?.moveCamera(
                CameraUpdateFactory.newLatLng(
                    LatLng(
                        java.lang.Double.valueOf(model.lat),
                        java.lang.Double.valueOf(model.lan)
                    )
                )
            )


        } else {
            marker?.setIcon(BitmapDescriptorFactory.fromBitmap(loadBitmapFromView(view)))
            animateMarker(
                marker!!, LatLng(
                    model.lat.toDouble(),
                    model.lan.toDouble()
                ), LatLngInterpolator.Spherical()
            )
        }

    }

    fun loadBitmapFromView(v: View): Bitmap? {
        val b = Bitmap.createBitmap(
            v.width,
            v.height,
            Bitmap.Config.ARGB_8888
        )
        val c = Canvas(b)
        v.layout(v.left, v.top, v.right, v.bottom)
        v.draw(c)
        return b
    }


    /** update marker locations by tcp response**/
    fun updateMarkerLocation(sResponse: String) {
        // val listUsers: List<String> = str.replace(UPDATE, "", true).split(",")
        Log.d("Updating..", "" + sResponse)
        var response = sResponse.replace(UPDATE, "", true)
        var listUsers: List<String>? = null
        val listLocation: List<String> = response.split(";")
        for (i in listLocation.indices) {
            if (listLocation[i].trim().isNotEmpty()) {

                listUsers = listLocation[i].split(",")
                Log.d("update_1_", "" + listUsers[1] + "__" + listUsers[2])
                addMarker(
                    UserModel(
                        listUsers[0], //Id
                        "", //name
                        "", //profile
                        listUsers[1], //lat
                        listUsers[2] //lan
                        , null, false
                    ), true
                )
            }
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        this.googleMap = map
        mTcpClient?.sendMessage("$AUTHORIZE $email")
    }
}