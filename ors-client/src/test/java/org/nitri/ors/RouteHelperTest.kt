package org.nitri.ors

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.nitri.ors.domain.route.Route
import org.nitri.ors.domain.route.RouteRequest
import org.nitri.ors.domain.route.RouteResponse
import org.nitri.ors.domain.route.RouteSummary
import org.nitri.ors.helper.RouteHelper

class RouteHelperTest {

    private lateinit var client: OrsClient
    private lateinit var routeHelper: RouteHelper

    @Before
    fun setUp() {
        client = mock(OrsClient::class.java)
        routeHelper = RouteHelper()
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

        `when`(client.getRoute(Profile.DRIVING_CAR, expectedRequest)).thenReturn(expectedResponse)

        val result = with(routeHelper) { client.getRoute(start, end, profile) }

        assert(result == expectedResponse)
        verify(client).getRoute(Profile.DRIVING_CAR, expectedRequest)
    }
}
