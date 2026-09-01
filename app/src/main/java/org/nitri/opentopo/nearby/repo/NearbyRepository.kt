package org.nitri.opentopo.nearby.repo

import android.text.TextUtils
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
    private val mLongitude: Double,
) {

    val items: Flow<List<NearbyItem>>
        get() = mDao?.observeAll() ?: flowOf(emptyList())

    @WorkerThread
    fun refresh(viewModelScope: CoroutineScope) {
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
                    if (!response.isSuccessful) {
                        val errorBody = try {
                            response.errorBody()?.string()
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to read Wikimedia error body", e)
                            null
                        }

                        Log.e(
                            TAG,
                            "Wikimedia request failed: HTTP ${response.code()}" +
                                    if (errorBody.isNullOrBlank()) "" else ", body=$errorBody"
                        )
                        return
                    }

                    val body = response.body()
                    if (body == null) {
                        Log.e(TAG, "Wikimedia request succeeded but returned an empty body")
                        return
                    }

                    viewModelScope.launch(Dispatchers.IO) {
                        insertNearby(body)
                    }
                }

                override fun onFailure(call: Call<MediaWikiResponse?>, t: Throwable) {
                    Log.e(TAG, "Wikimedia Nearby request failed", t)
                }
            })

        }
    }

    private fun insertNearby(mediaWikiResponse: MediaWikiResponse?) {
        mediaWikiResponse?.query?.pages?.let { pages ->
            val items = pages.mapNotNull { (key, page) ->
                NearbyItem().apply {
                    pageid = key
                    index = page.index
                    title = page.title
                    url =
                        if (TextUtils.isEmpty(page.canonicalurl)) page.fullurl else page.canonicalurl
                    page.coordinates?.firstOrNull()?.let { coords ->
                        lat = coords.lat
                        lon = coords.lon
                    }
                    description = page.terms?.description?.firstOrNull()
                    page.thumbnail?.let { thumb ->
                        thumbnail = thumb.source
                        width = thumb.width
                        height = thumb.height
                    }
                }
            }.toTypedArray()

            mDao?.replaceAll(items.toList())

        } ?: return
    }

    companion object {
        private val TAG = NearbyRepository::class.java.simpleName
    }
}
