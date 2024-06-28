package org.nitri.opentopo

import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowMetrics
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : BaseMainActivity() {

    private lateinit var adViewContainer: ViewGroup
    private var adView: AdView? = null
    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val initialLayoutComplete = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adViewContainer = findViewById(R.id.ad_view_container)
//        val backgroundScope = CoroutineScope(Dispatchers.IO)
//        backgroundScope.launch {
//            MobileAds.initialize(this@MainActivity) {}
//        }

        // Log the Mobile Ads SDK version.
        Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())

        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)
        googleMobileAdsConsentManager.gatherConsent(this) { error ->
            if (error != null) {
                // Consent not obtained in current session.
                Log.d(TAG, "${error.errorCode}: ${error.message}")
            }

            if (googleMobileAdsConsentManager.canRequestAds) {
                initializeMobileAdsSdk()
            }

            if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
                // Regenerate the options menu to include a privacy setting.
                invalidateOptionsMenu()
            }
        }

        // This sample attempts to load ads using consent obtained in the previous session.
        if (googleMobileAdsConsentManager.canRequestAds) {
            initializeMobileAdsSdk()
        }

        // Since we're loading the banner based on the adContainerView size, we need to wait until this
        // view is laid out before we can get the width.
        adViewContainer.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialLayoutComplete.getAndSet(true) && googleMobileAdsConsentManager.canRequestAds) {
                loadBanner()
            }
        }
    }

    // Determine the screen width (less decorations) to use for the ad width.
    // If the ad hasn't been laid out, default to the full screen width.
    private val adSize: AdSize
        get() {
            val displayMetrics = resources.displayMetrics
            val adWidthPixels = adViewContainer.width.toFloat().takeIf { it != 0f } ?: run {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    windowManager.currentWindowMetrics.bounds.width().toFloat()
                } else {
                    displayMetrics.widthPixels.toFloat()
                }
            }

            val adWidth = (adWidthPixels / displayMetrics.density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    public override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    public override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }

    private fun loadBanner() {

        // Create and configure a new ad view.
        val adView = AdView(this)
        adView.adUnitId = getString(R.string.ad_unit_id)
        adView.setAdSize(adSize)
        this.adView = adView

        // Replace ad container with the new ad view.
        adViewContainer.apply {
            removeAllViews()
            addView(adView)
        }

        // Start loading the ad in the background.
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // Set your test devices.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(listOf(getString(R.string.test_device_hashed_id))).build()
        )

        CoroutineScope(Dispatchers.IO).launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(this@MainActivity) {}

            runOnUiThread {
                // Load an ad on the main thread.
                if (initialLayoutComplete.get()) {
                    loadBanner()
                }
            }
        }
    }

    override fun toggleFullscreen() {
        super.toggleFullscreen()
        adView?.visibility = if (isFullscreen) View.GONE else View.VISIBLE
    }

    override fun isPrivacyOptionsRequired(): Boolean {
        return googleMobileAdsConsentManager.isPrivacyOptionsRequired
    }

    override fun showPrivacyOptionsForm() {
        googleMobileAdsConsentManager.showPrivacyOptionsForm(this@MainActivity) { formError ->
            if (formError != null) {
                Toast.makeText(this@MainActivity, formError.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}