package org.nitri.opentopo.nearby.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.nitri.opentopo.nearby.entity.NearbyItem
import org.nitri.opentopo.nearby.repo.NearbyRepository

class NearbyViewModel : ViewModel() {
    private var mRepository: NearbyRepository? = null
    fun setRepository(repository: NearbyRepository?) {
        mRepository = repository
    }

    val items: LiveData<List<NearbyItem>>
        get() = mRepository!!.loadNearbyItems()
}
