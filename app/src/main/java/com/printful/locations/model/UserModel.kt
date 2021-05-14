package com.printful.locations.model

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

data class UserModel(
    val id: String,
    var name: String,
    var profile: String,
    var lat: String,
    var lan: String,
    var image: Drawable?,
    var isImageLoaded:Boolean
)