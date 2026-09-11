package org.nitri.opentopo.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nitri.ors.OrsClient
import org.nitri.ors.Profile
import org.nitri.ors.domain.elevation.ElevationLineRequest
import org.nitri.ors.domain.elevation.ElevationLineResponse
import org.nitri.ors.domain.elevation.ElevationPointRequest
import org.nitri.ors.domain.elevation.ElevationPointResponse
import org.nitri.ors.domain.export.ExportRequest
import org.nitri.ors.domain.export.ExportResponse
import org.nitri.ors.domain.export.TopoJsonExportResponse
import org.nitri.ors.domain.geocode.GeocodeFeature
import org.nitri.ors.domain.geocode.GeocodeGeometry
import org.nitri.ors.domain.geocode.GeocodeProperties
import org.nitri.ors.domain.geocode.GeocodeSearchResponse
import org.nitri.ors.domain.isochrones.IsochronesRequest
import org.nitri.ors.domain.isochrones.IsochronesResponse
import org.nitri.ors.domain.matrix.MatrixRequest
import org.nitri.ors.domain.matrix.MatrixResponse
import org.nitri.ors.domain.optimization.OptimizationRequest
import org.nitri.ors.domain.optimization.OptimizationResponse
import org.nitri.ors.domain.pois.PoisGeoJsonResponse
import org.nitri.ors.domain.pois.PoisRequest
import org.nitri.ors.domain.route.GeoJsonRouteResponse
import org.nitri.ors.domain.route.RouteRequest
import org.nitri.ors.domain.route.RouteResponse
import org.nitri.ors.domain.snap.SnapGeoJsonResponse
import org.nitri.ors.domain.snap.SnapRequest
import org.nitri.ors.domain.snap.SnapResponse

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceSearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeOrsClient(
        var onAutocomplete: (suspend (text: String) -> GeocodeSearchResponse)? = null
    ) : OrsClient {
        var callCount = 0
        var lastQuery: String? = null

        override suspend fun geocodeAutocomplete(
            text: String,
            focusLon: Double?,
            focusLat: Double?,
            rectMinLon: Double?,
            rectMinLat: Double?,
            rectMaxLon: Double?,
            rectMaxLat: Double?,
            circleLon: Double?,
            circleLat: Double?,
            circleRadius: Double?,
            country: String?,
            sources: List<String>?,
            layers: List<String>?,
            size: Int?
        ): GeocodeSearchResponse {
            callCount++
            lastQuery = text
            return onAutocomplete?.invoke(text) ?: GeocodeSearchResponse(features = emptyList())
        }

        override suspend fun getRoute(profile: Profile, routeRequest: RouteRequest): RouteResponse = TODO()
        override suspend fun getRouteGpx(profile: Profile, routeRequest: RouteRequest): String = TODO()
        override suspend fun getRouteGpx(profile: Profile, routeRequest: RouteRequest, includeElevation: Boolean): String = TODO()
        override suspend fun getRouteGeoJson(profile: Profile, routeRequest: RouteRequest): GeoJsonRouteResponse = TODO()
        override suspend fun export(profile: Profile, exportRequest: ExportRequest): ExportResponse = TODO()
        override suspend fun exportJson(profile: Profile, exportRequest: ExportRequest): ExportResponse = TODO()
        override suspend fun exportTopoJson(profile: Profile, exportRequest: ExportRequest): TopoJsonExportResponse = TODO()
        override suspend fun getIsochrones(profile: Profile, isochronesRequest: IsochronesRequest): IsochronesResponse = TODO()
        override suspend fun getMatrix(profile: Profile, matrixRequest: MatrixRequest): MatrixResponse = TODO()
        override suspend fun getSnap(profile: Profile, snapRequest: SnapRequest): SnapResponse = TODO()
        override suspend fun getSnapJson(profile: Profile, snapRequest: SnapRequest): SnapResponse = TODO()
        override suspend fun getSnapGeoJson(profile: Profile, snapRequest: SnapRequest): SnapGeoJsonResponse = TODO()
        override suspend fun getPois(poisRequest: PoisRequest): PoisGeoJsonResponse = TODO()
        override suspend fun getOptimization(optimizationRequest: OptimizationRequest): OptimizationResponse = TODO()
        override suspend fun getElevationLine(elevationLineRequest: ElevationLineRequest): ElevationLineResponse = TODO()
        override suspend fun getElevationPoint(elevationPointRequest: ElevationPointRequest): ElevationPointResponse = TODO()
        override suspend fun geocodeSearch(text: String, focusLon: Double?, focusLat: Double?, rectMinLon: Double?, rectMinLat: Double?, rectMaxLon: Double?, rectMaxLat: Double?, circleLon: Double?, circleLat: Double?, circleRadiusMeters: Double?, boundaryGid: String?, boundaryCountry: String?, sourcesCsv: String?, layersCsv: String?, size: Int?): GeocodeSearchResponse = TODO()
        override suspend fun geocodeReverse(lon: Double, lat: Double, radiusKm: Double?, size: Int?, layers: List<String>?, sources: List<String>?, boundaryCountry: String?): GeocodeSearchResponse = TODO()
        override suspend fun geocodeStructured(address: String?, neighbourhood: String?, borough: String?, locality: String?, county: String?, region: String?, country: String?, postalcode: String?, focusLon: Double?, focusLat: Double?, rectMinLon: Double?, rectMinLat: Double?, rectMaxLon: Double?, rectMaxLat: Double?, circleLon: Double?, circleLat: Double?, circleRadiusMeters: Double?, boundaryCountry: String?, layers: List<String>?, sources: List<String>?, size: Int?): GeocodeSearchResponse = TODO()
    }

    @Test
    fun queryUnderThreeChars_makesNoRequest() = runTest {
        val fakeClient = FakeOrsClient()
        val viewModel = PlaceSearchViewModel({ fakeClient }, 6.0, 51.0)

        viewModel.setQuery("ab")
        advanceUntilIdle()

        assertEquals(0, fakeClient.callCount)
        assertEquals(PlaceSearchUiState.QueryTooShort, viewModel.uiState.value)
    }

    @Test
    fun debounce_doesNotRequestBefore400ms() = runTest {
        val fakeClient = FakeOrsClient()
        val viewModel = PlaceSearchViewModel({ fakeClient }, 6.0, 51.0)

        viewModel.setQuery("Kleve")
        advanceTimeBy(399)

        assertEquals(0, fakeClient.callCount)

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(1, fakeClient.callCount)
        assertEquals("Kleve", fakeClient.lastQuery)
    }

    @Test
    fun rapidQueryChanges_executesOnlyLatest() = runTest {
        val fakeClient = FakeOrsClient { text ->
            GeocodeSearchResponse(
                features = listOf(
                    GeocodeFeature(
                        geometry = GeocodeGeometry(coordinates = listOf(6.0, 51.0)),
                        properties = GeocodeProperties(name = text, label = text)
                    )
                )
            )
        }
        val viewModel = PlaceSearchViewModel({ fakeClient }, 6.0, 51.0)

        viewModel.setQuery("Klev")
        advanceTimeBy(200)
        viewModel.setQuery("Kleve")
        advanceTimeBy(200)
        viewModel.setQuery("Kleve City")

        advanceUntilIdle()

        assertEquals(1, fakeClient.callCount)
        assertEquals("Kleve City", fakeClient.lastQuery)
        assertTrue(viewModel.uiState.value is PlaceSearchUiState.Success)
    }

    @Test
    fun runningRequestIsCancelledByNewQuery() = runTest {
        var queryACancelled = false

        val fakeClient = FakeOrsClient { text ->
            if (text == "QueryA") {
                suspendCancellableCoroutine { continuation ->
                    continuation.invokeOnCancellation {
                        queryACancelled = true
                    }
                }
            } else {
                GeocodeSearchResponse(
                    features = listOf(
                        GeocodeFeature(
                            geometry = GeocodeGeometry(coordinates = listOf(3.0, 4.0)),
                            properties = GeocodeProperties(name = "ResultB", label = "ResultB")
                        )
                    )
                )
            }
        }

        val viewModel = PlaceSearchViewModel({ fakeClient }, 6.0, 51.0)

        viewModel.setQuery("QueryA")
        advanceTimeBy(400)
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value is PlaceSearchUiState.Loading)

        viewModel.setQuery("QueryB")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertTrue(queryACancelled)
        val state = viewModel.uiState.value
        assertTrue(state is PlaceSearchUiState.Success)
        val results = (state as PlaceSearchUiState.Success).results
        assertEquals(1, results.size)
        assertEquals("ResultB", results[0].name)
    }

    @Test
    fun repeatedIdenticalQuery_doesNotRepeatRequest() = runTest {
        val fakeClient = FakeOrsClient()
        val viewModel = PlaceSearchViewModel({ fakeClient }, 6.0, 51.0)

        viewModel.setQuery("Kleve")
        advanceUntilIdle()
        assertEquals(1, fakeClient.callCount)

        viewModel.setQuery("Kleve")
        advanceUntilIdle()
        assertEquals(1, fakeClient.callCount)
    }

    @Test
    fun returningBelowThreeChars_clearsResults() = runTest {
        val fakeClient = FakeOrsClient {
            GeocodeSearchResponse(
                features = listOf(
                    GeocodeFeature(
                        geometry = GeocodeGeometry(coordinates = listOf(6.0, 51.0)),
                        properties = GeocodeProperties(name = "Kleve", label = "Kleve")
                    )
                )
            )
        }
        val viewModel = PlaceSearchViewModel({ fakeClient }, 6.0, 51.0)

        viewModel.setQuery("Kleve")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is PlaceSearchUiState.Success)

        viewModel.setQuery("Kl")
        advanceUntilIdle()
        assertEquals(PlaceSearchUiState.QueryTooShort, viewModel.uiState.value)
    }

    @Test
    fun emptyResponse_producesEmptyState() = runTest {
        val fakeClient = FakeOrsClient {
            GeocodeSearchResponse(features = emptyList())
        }
        val viewModel = PlaceSearchViewModel({ fakeClient }, 6.0, 51.0)

        viewModel.setQuery("NonExistentPlace12345")
        advanceUntilIdle()

        assertEquals(1, fakeClient.callCount)
        assertEquals(PlaceSearchUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun serviceFailure_producesErrorState() = runTest {
        val fakeClient = FakeOrsClient {
            throw RuntimeException("Network error")
        }
        val viewModel = PlaceSearchViewModel({ fakeClient }, 6.0, 51.0)

        viewModel.setQuery("Kleve")
        advanceUntilIdle()

        assertEquals(1, fakeClient.callCount)
        assertTrue(viewModel.uiState.value is PlaceSearchUiState.Error)
    }

    @Test
    fun retry_reissuesRequestWithoutModifyingQueryText() = runTest {
        var shouldFail = true
        val fakeClient = FakeOrsClient { text ->
            if (shouldFail) {
                throw RuntimeException("Network error")
            } else {
                GeocodeSearchResponse(
                    features = listOf(
                        GeocodeFeature(
                            geometry = GeocodeGeometry(coordinates = listOf(6.0, 51.0)),
                            properties = GeocodeProperties(name = text, label = text)
                        )
                    )
                )
            }
        }

        val viewModel = PlaceSearchViewModel({ fakeClient }, 6.0, 51.0)

        viewModel.setQuery("Kleve")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(1, fakeClient.callCount)
        assertTrue(viewModel.uiState.value is PlaceSearchUiState.Error)
        assertEquals("Kleve", viewModel.query.value)

        shouldFail = false
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, fakeClient.callCount)
        assertEquals("Kleve", viewModel.query.value)
        assertTrue(viewModel.uiState.value is PlaceSearchUiState.Success)
        val success = viewModel.uiState.value as PlaceSearchUiState.Success
        assertEquals("Kleve", success.results[0].name)
    }
}
