package org.nitri.opentopo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import org.nitri.opentopo.model.PlaceSearchResult
import org.nitri.opentopo.analytics.AnalyticsTracker
import org.nitri.opentopo.analytics.NoOpAnalyticsTracker
import org.nitri.opentopo.analytics.OrsOutcome
import org.nitri.opentopo.analytics.classifyOrsError
import org.nitri.opentopo.analytics.durationBucket
import org.nitri.opentopo.analytics.queryLengthBucket
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
    private val focusLat: Double?,
    private val analytics: AnalyticsTracker = NoOpAnalyticsTracker,
    private val clockMillis: () -> Long = { System.nanoTime() / 1_000_000 }
) : ViewModel() {

    private data class SearchRequest(val query: String, val isRetry: Boolean)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<PlaceSearchUiState>(PlaceSearchUiState.QueryTooShort)
    val uiState: StateFlow<PlaceSearchUiState> = _uiState.asStateFlow()

    private val _retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        val debouncedQuery = _query
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce { normalizedQuery ->
                if (normalizedQuery.length >= 3) 400L else 0L
            }
            .map { SearchRequest(it, false) }

        val retryQuery = _retryTrigger.map { SearchRequest(_query.value.trim(), true) }

        merge(debouncedQuery, retryQuery)
            .flatMapLatest { request ->
                flow {
                    val normalizedQuery = request.query
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

                    val startedAt = clockMillis()
                    try {
                        val response = client.geocodeAutocomplete(
                            text = normalizedQuery,
                            focusLon = focusLon,
                            focusLat = focusLat,
                            size = 10
                        )
                        val results = mapGeocodeFeaturesToPlaceSearchResults(response.features)
                        val outcome = if (results.isEmpty()) OrsOutcome.EMPTY else OrsOutcome.SUCCESS
                        analytics.trackOrsSearchResult(outcome, results.size.coerceAtMost(10),
                            durationBucket(clockMillis() - startedAt), queryLengthBucket(normalizedQuery.length),
                            request.isRetry)
                        if (results.isEmpty()) {
                            emit(PlaceSearchUiState.Empty)
                        } else {
                            emit(PlaceSearchUiState.Success(results))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        analytics.trackOrsSearchResult(OrsOutcome.ERROR, 0,
                            durationBucket(clockMillis() - startedAt), queryLengthBucket(normalizedQuery.length),
                            request.isRetry, classifyOrsError(e))
                        emit(PlaceSearchUiState.Error(null))
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
        _retryTrigger.tryEmit(Unit)
    }

    class Factory(
        private val clientProvider: () -> OrsClient?,
        private val focusLon: Double?,
        private val focusLat: Double?,
        private val analytics: AnalyticsTracker = NoOpAnalyticsTracker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlaceSearchViewModel(clientProvider, focusLon, focusLat, analytics) as T
        }
    }
}
