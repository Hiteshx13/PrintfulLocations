package com.printful.locations.utils

import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import android.os.SystemClock
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.printful.locations.R
import com.printful.locations.`interface`.LatLngInterpolator
import java.util.*


var UPDATE = "update"
var USERLIST = "userlist"
var PARAM_EMAIL = "param_email"
var AUTHORIZE = "AUTHORIZE"
var ANIM_TIME_IMAGE: Long = 5000
var ANIM_SHORT_TIME_IMAGE: Long = 2500

/**show toast message**/
fun showTast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

/**Start new activity
 * This saperate function will be useful if we needs to
 * add common animation for lounch activity**/
fun lounchActivity(context: Context, intent: Intent) {
    context.startActivity(intent)
}

fun getAddressFromLatLan(
    context: Context,
    latitude: String,
    longitude: String
): String {
    val addresses: List<Address>
    val geocoder = Geocoder(context, Locale.getDefault())
    addresses = geocoder.getFromLocation(
        latitude.toDouble(),
        longitude.toDouble(),
        1
    )

    val address: String = addresses[0]
        .getAddressLine(0)
    return address
}


fun animateMarker(
    marker: Marker,
    finalPosition: LatLng,
    latLngInterpolator: LatLngInterpolator
) {
    val startPosition = marker.position
    val handler = Handler()
    val start = SystemClock.uptimeMillis()
    val interpolator: Interpolator = AccelerateDecelerateInterpolator()
    val durationInMs = 2000f
    handler.post(object : Runnable {
        var elapsed: Long = 0
        var t = 0f
        var v = 0f
        override fun run() {
            // Calculate progress using interpolator
            elapsed = SystemClock.uptimeMillis() - start
            t = elapsed / durationInMs
            v = interpolator.getInterpolation(t)
            marker.position = latLngInterpolator.interpolate(v, startPosition, finalPosition)
            // Repeat till progress is complete.
            if (t < 1) {
                // Post again 16ms later.
                handler.postDelayed(this, 16)
            }
        }
    })
}

fun animateFlip(context: Context, view: View, millisecond: Long) {
    val animFlip =
        AnimatorInflater.loadAnimator(context, R.animator.anim_flip) as ObjectAnimator

    val handler = Handler()
    handler.postDelayed(object : Runnable {
        override fun run() {
            animFlip.target = view
            animFlip.duration = millisecond
            animFlip.start()
            handler.postDelayed(this, millisecond)
        }
    }, 0)
}

fun isNetworkConnected(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
    val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true

    return isConnected
}