package org.nitri.ors


import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.model.route.Route
import org.nitri.ors.model.route.RouteRequest
import org.nitri.ors.model.route.RouteResponse
import org.nitri.ors.model.route.RouteSummary
import org.nitri.ors.repository.RouteRepository

class RouteRepositoryTest {

    private lateinit var api: OpenRouteServiceApi
    private lateinit var repository: RouteRepository

    @Before
    fun setUp() {
        api = mock(OpenRouteServiceApi::class.java)
        repository = RouteRepository(api)
    }

    @Test
    fun `getRoute returns response from API`() = runTest {
        val start = Pair(8.681495, 49.41461)
        val end = Pair(8.687872, 49.420318)
        val profile = "driving-car"

        val expectedResponse = RouteResponse(
            routes = listOf(
                Route(
                    summary = RouteSummary(1000.0, 600.0),
                    geometry = "encodedPolyline",
                    segments = emptyList()
                )
            ),
            bbox = listOf(0.0, 0.0, 1.0, 1.0)
        )

        val expectedRequest = RouteRequest(
            coordinates = listOf(
                listOf(start.first, start.second),
                listOf(end.first, end.second)
            )
        )

        `when`(api.getRoute(profile, expectedRequest)).thenReturn(expectedResponse)

        val result = repository.getRoute(start, end, profile)

        assert(result == expectedResponse)
        verify(api).getRoute(profile, expectedRequest)
    }
}
