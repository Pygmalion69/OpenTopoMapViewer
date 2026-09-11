package org.nitri.opentopo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.nitri.opentopo.model.PlaceSearchResult
import org.nitri.opentopo.ors.mapGeocodeFeaturesToPlaceSearchResults
import org.nitri.ors.OrsClient
import kotlin.coroutines.cancellation.CancellationException

sealed interface PlaceSearchUiState {
    data object QueryTooShort : PlaceSearchUiState
    data object Loading : PlaceSearchUiState
    data class Success(val results: List<PlaceSearchResult>) : PlaceSearchUiState
    data object Empty : PlaceSearchUiState
    data class Error(val message: String?) : PlaceSearchUiState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PlaceSearchViewModel(
    private val clientProvider: () -> OrsClient?,
    private val focusLon: Double?,
    private val focusLat: Double?
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<PlaceSearchUiState>(PlaceSearchUiState.QueryTooShort)
    val uiState: StateFlow<PlaceSearchUiState> = _uiState.asStateFlow()

    init {
        _query
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce { normalizedQuery ->
                if (normalizedQuery.length >= 3) 400L else 0L
            }
            .flatMapLatest { normalizedQuery ->
                flow {
                    if (normalizedQuery.length < 3) {
                        emit(PlaceSearchUiState.QueryTooShort)
                        return@flow
                    }

                    val client = clientProvider()
                    if (client == null) {
                        emit(PlaceSearchUiState.Error(null))
                        return@flow
                    }

                    emit(PlaceSearchUiState.Loading)

                    try {
                        val response = client.geocodeAutocomplete(
                            text = normalizedQuery,
                            focusLon = focusLon,
                            focusLat = focusLat,
                            size = 10
                        )
                        val results = mapGeocodeFeaturesToPlaceSearchResults(response.features)
                        if (results.isEmpty()) {
                            emit(PlaceSearchUiState.Empty)
                        } else {
                            emit(PlaceSearchUiState.Success(results))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        emit(PlaceSearchUiState.Error(e.localizedMessage))
                    }
                }
            }
            .onEach { newState ->
                _uiState.value = newState
            }
            .launchIn(viewModelScope)
    }

    fun setQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun retry() {
        val current = _query.value
        _query.value = ""
        _query.value = current
    }

    class Factory(
        private val clientProvider: () -> OrsClient?,
        private val focusLon: Double?,
        private val focusLat: Double?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlaceSearchViewModel(clientProvider, focusLon, focusLat) as T
        }
    }
}
