package org.nitri.opentopo.nearby.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import org.nitri.opentopo.nearby.entity.NearbyItem;
import org.nitri.opentopo.nearby.repo.NearbyRepository;

import java.util.List;

/**
 * Created by helfrich on 25/02/2018.
 */

public class NearbyViewModel extends ViewModel {

    private NearbyRepository mRepository;

    public void setRepository(NearbyRepository repository) {
        mRepository = repository;
    }

    public LiveData<List<NearbyItem>> getItems() {
        return mRepository.loadNearbyItems();
    }

}
