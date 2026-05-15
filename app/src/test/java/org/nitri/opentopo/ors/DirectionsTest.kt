package org.nitri.opentopo.ors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.nitri.ors.OrsClient
import org.nitri.ors.Profile
import org.nitri.ors.domain.route.RouteRequest

@OptIn(ExperimentalCoroutinesApi::class)
class DirectionsTest {

    @Mock
    private lateinit var mockOrsClient: OrsClient

    @Mock
    private lateinit var mockResult: Directions.RouteGpxResult

    private lateinit var directions: Directions
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        directions = Directions(mockOrsClient, "driving-car")
    }

    @Test
    fun getRouteGpx_success_callsOnSuccess() = runTest {
        val gpxString = "<gpx></gpx>"
        // Use doAnswer for suspend functions to ensure it works correctly with coroutines
        doAnswer { gpxString }.`when`(mockOrsClient).getRouteGpx(any(), any(), eq(true))

        directions.getRouteGpx(listOf(listOf(1.0, 2.0)), "en", mockResult)
        
        advanceUntilIdle()
        verify(mockResult).onSuccess(gpxString)
    }

    @Test
    fun getRouteGpx_emptyResponse_callsOnError() = runTest {
        doAnswer { "" }.`when`(mockOrsClient).getRouteGpx(any(), any(), eq(true))

        directions.getRouteGpx(listOf(listOf(1.0, 2.0)), "en", mockResult)

        advanceUntilIdle()
        verify(mockResult).onError("Empty response body")
    }

    @Test
    fun getRouteGpx_exception_callsOnError() = runTest {
        doAnswer { throw RuntimeException("Network error") }.`when`(mockOrsClient).getRouteGpx(any(), any(), eq(true))

        directions.getRouteGpx(listOf(listOf(1.0, 2.0)), "en", mockResult)

        advanceUntilIdle()
        verify(mockResult).onError("Failed to fetch GPX: Network error")
    }
}
