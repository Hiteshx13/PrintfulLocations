package com.printful.locations.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.model.Marker
import com.printful.locations.R
import com.printful.locations.model.UserModel
import com.printful.locations.utils.getAddressFromLatLan


class InfoWindowAdapter(
    var context: Context

) : InfoWindowAdapter {

    var currentMarkerID = ""
    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    override fun getInfoWindow(marker: Marker): View? {
        /**isLoading is status that loading image from url**/
        var isLoading = true
        var userModel: UserModel = marker.tag as UserModel
        val inflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.row_info_window, null)
        val ivProfile = view.findViewById(R.id.ivProfile) as AppCompatImageView

        Glide
            .with(context)
            .load(marker.snippet)
            .placeholder(R.mipmap.ic_launcher)
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
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {

                    /**this condition ensure that we only enter if image is not loaded
                     * otherwise it will be recursion **/
                    if (isLoading && !currentMarkerID.equals(marker.id)) {
                        isLoading = false
                        currentMarkerID = marker.id
                        marker.showInfoWindow()
                    }
                    return false
                }
            })
            .into(ivProfile)

        val tvName = view.findViewById(R.id.tvName) as TextView
        val tvAddress = view.findViewById(R.id.tvAddress) as TextView
        tvName.text = userModel.name
        tvAddress.text =
            getAddressFromLatLan(context, userModel.lat, userModel.lan).replace(",", "\n")

        return view
    }
}