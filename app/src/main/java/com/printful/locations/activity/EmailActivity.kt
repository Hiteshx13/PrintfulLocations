package com.printful.locations.activity

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.printful.locations.R
import com.printful.locations.databinding.ActivityEmailBinding
import com.printful.locations.utils.ANIM_TIME_IMAGE
import com.printful.locations.utils.animateFlip
import com.printful.locations.utils.lounchActivity
import com.printful.locations.utils.showTast

class EmailActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var binding: ActivityEmailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_email)
        binding.btnProceed.setOnClickListener(this)
        animateFlip(
            this,
            binding.ivMarker,
            ANIM_TIME_IMAGE
        )
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnProceed -> {

                var strEmail = binding.etEmail.text.toString().trim()
                if (!strEmail.isNullOrEmpty() &&
                    Patterns.EMAIL_ADDRESS.matcher(strEmail).matches()
                ) {
                    lounchActivity(this,MapActivity.getIntent(this,strEmail))
                } else {
                    showTast(this,getString(R.string.enter_valid_email))
                }
            }
        }
    }

}