package org.nitri.opentopo.nearby.repo

import android.text.TextUtils
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import org.nitri.opentopo.nearby.api.mediawiki.MediaWikiApi
import org.nitri.opentopo.nearby.api.mediawiki.MediaWikiResponse
import org.nitri.opentopo.nearby.da.NearbyDao
import org.nitri.opentopo.nearby.entity.NearbyItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NearbyRepository(
    private val mDao: NearbyDao?,
    private val mApi: MediaWikiApi?,
    private val mLatitude: Double,
    private val mLongitude: Double
) {
    private val TAG = NearbyRepository::class.java.simpleName
    fun loadNearbyItems(): LiveData<List<NearbyItem>> {
        refresh()
        return mDao!!.loadAll()
    }

    @WorkerThread
    private fun refresh() {
        if (mApi != null) {
            val call = mApi.getNearbyPages(
                "query", "coordinates|pageimages|pageterms|info",
                50, "thumbnail", 60, 50, "description", "geosearch",
                "$mLatitude|$mLongitude", 10000, 50, "url", "json"
            )
            call?.enqueue(object : Callback<MediaWikiResponse?> {
                override fun onResponse(
                    call: Call<MediaWikiResponse?>,
                    response: Response<MediaWikiResponse?>
                ) {
                    Log.d(TAG, response.toString())
                    if (response.body() != null) {
                        insertNearby(response.body())
                    }
                }

                override fun onFailure(call: Call<MediaWikiResponse?>, t: Throwable) {
                    Log.e(TAG, "refresh failed", t)
                }
            })
        }
    }

    private fun insertNearby(mediaWikiResponse: MediaWikiResponse?) {
        if (mediaWikiResponse == null || mDao == null || mediaWikiResponse.query == null || mediaWikiResponse.query!!.pages == null) {
            return
        }
        val array = mediaWikiResponse.query?.pages?.size?.let { arrayOfNulls<NearbyItem>(it) }
        var index = 0
        for ((key, page) in mediaWikiResponse.query!!.pages!!) {
            val item = NearbyItem()
            item.pageid = key
            item.index = page.index
            item.title = page.title
            item.url =
                if (TextUtils.isEmpty(page.canonicalurl)) page.fullurl else page.canonicalurl
            if (page.coordinates != null) {
                item.lat = page.coordinates!![0].lat
                item.lon = page.coordinates!![0].lon
            }
            if (page.terms != null) {
                item.description = page.terms!!.description?.get(0)
            }
            if (page.thumbnail != null) {
                item.thumbnail = page.thumbnail!!.source
                item.width = page.thumbnail!!.width
                item.height = page.thumbnail!!.height
            }
            array?.set(index, item)
            index++
        }
        Thread {
            mDao.delete() // Fill with fresh nearby data
            array?.let { items ->
                mDao.insertItems(*items.filterNotNull().toTypedArray())
            }
        }.start()
    }
}
