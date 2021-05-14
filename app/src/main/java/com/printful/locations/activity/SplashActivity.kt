package com.printful.locations.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.printful.locations.R
import com.printful.locations.databinding.ActivitySplashBinding
import com.printful.locations.utils.ANIM_SHORT_TIME_IMAGE
import com.printful.locations.utils.ANIM_TIME_IMAGE
import com.printful.locations.utils.animateFlip

class SplashActivity : AppCompatActivity() {
    private lateinit var binding:ActivitySplashBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        binding=DataBindingUtil.setContentView(this,R.layout.activity_splash)
        animateFlip(
                this,
                binding.tvHeader,
            ANIM_SHORT_TIME_IMAGE
        )
    }

    override fun onResume() {
        super.onResume()
        initialization()
    }

    private fun initialization() {
        Handler().postDelayed({
            var intent = Intent(this@SplashActivity, EmailActivity::class.java)
            startActivity(intent)
            finish()
        }, SPLASH_TIME_OUT.toLong())
    }

    companion object {
        private const val SPLASH_TIME_OUT = 3000
    }
}