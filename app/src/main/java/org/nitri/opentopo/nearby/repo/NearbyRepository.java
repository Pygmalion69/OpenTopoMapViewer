package org.nitri.opentopo.nearby.repo;

import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import org.nitri.opentopo.nearby.api.mediawiki.MediaWikiApi;
import org.nitri.opentopo.nearby.api.mediawiki.MediaWikiResponse;
import org.nitri.opentopo.nearby.api.mediawiki.Page;
import org.nitri.opentopo.nearby.da.NearbyDao;
import org.nitri.opentopo.nearby.entity.NearbyItem;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NearbyRepository {

    private final String TAG = NearbyRepository.class.getSimpleName();

    private final NearbyDao mDao;
    private final MediaWikiApi mApi;
    private final double mLatitude;
    private final double mLongitude;

    public NearbyRepository(NearbyDao dao, MediaWikiApi api, double latitude, double longitude) {
        mDao = dao;
        mApi = api;
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public LiveData<List<NearbyItem>> loadNearbyItems() {
        refresh();
        return mDao.loadAll();
    }

    @WorkerThread
    private void refresh() {

        if (mApi != null) {
            Call<MediaWikiResponse> call = mApi.getNearbyPages("query", "coordinates|pageimages|pageterms|info",
                    50, "thumbnail", 60, 50, "description", "geosearch",
                    mLatitude + "|" + mLongitude, 10000, 50, "url","json");
            call.enqueue(new Callback<MediaWikiResponse>() {
                @Override
                public void onResponse(@NonNull Call<MediaWikiResponse> call, @NonNull Response<MediaWikiResponse> response) {
                    Log.d(TAG, response.toString());
                    if (response.body() != null) {
                        insertNearby(response.body());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<MediaWikiResponse> call, @NonNull Throwable t) {
                    t.printStackTrace();
                }
            });
        }
    }

    private void insertNearby(MediaWikiResponse mediaWikiResponse) {
        if (mediaWikiResponse != null) {
            NearbyItem[] array = new NearbyItem[mediaWikiResponse.getQuery().getPages().size()];
            if (mDao != null) {
                int index = 0;
                for (Map.Entry<String, Page> pageEntry : mediaWikiResponse.getQuery().getPages().entrySet()) {
                    NearbyItem item = new NearbyItem();
                    item.setPageid(pageEntry.getKey());
                    Page page = pageEntry.getValue();
                    item.setIndex(page.getIndex());
                    item.setTitle(page.getTitle());
                    item.setUrl(TextUtils.isEmpty(page.getCanonicalurl()) ? page.getFullurl() : page.getCanonicalurl());
                    if (page.getCoordinates() != null) {
                        item.setLat(page.getCoordinates().get(0).getLat());
                        item.setLon(page.getCoordinates().get(0).getLon());
                    }
                    if (page.getTerms() != null) {
                        item.setDescription(page.getTerms().getDescription().get(0));
                    }
                    if (page.getThumbnail() != null) {
                        item.setThumbnail(page.getThumbnail().getSource());
                        item.setWidth(page.getThumbnail().getWidth());
                        item.setHeight(page.getThumbnail().getHeight());
                    }
                    array[index] = item;
                    index++;
                }
                new Thread(() -> {
                    mDao.delete(); // Fill with fresh nearby data
                    mDao.insertItems(array);
                }).start();
            }
        }

    }
}
