package org.nitri.opentopo.analytics

import com.google.gson.JsonSyntaxException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class OrsAnalyticsTest {
    @Test fun durationBoundaries() {
        assertEquals(DurationBucket.UNDER_1S, durationBucket(999))
        assertEquals(DurationBucket.ONE_TO_THREE_SECONDS, durationBucket(1_000))
        assertEquals(DurationBucket.ONE_TO_THREE_SECONDS, durationBucket(3_000))
        assertEquals(DurationBucket.OVER_3S, durationBucket(3_001))
    }

    @Test fun queryLengthBoundaries() {
        assertEquals(QueryLengthBucket.THREE_TO_FIVE, queryLengthBucket(3))
        assertEquals(QueryLengthBucket.THREE_TO_FIVE, queryLengthBucket(5))
        assertEquals(QueryLengthBucket.SIX_TO_TEN, queryLengthBucket(6))
        assertEquals(QueryLengthBucket.SIX_TO_TEN, queryLengthBucket(10))
        assertEquals(QueryLengthBucket.OVER_TEN, queryLengthBucket(11))
    }

    @Test fun resultPositionBoundaries() {
        assertEquals(ResultPositionBucket.FIRST, resultPositionBucket(0))
        assertEquals(ResultPositionBucket.SECOND_OR_THIRD, resultPositionBucket(1))
        assertEquals(ResultPositionBucket.SECOND_OR_THIRD, resultPositionBucket(2))
        assertEquals(ResultPositionBucket.FOURTH_OR_LATER, resultPositionBucket(3))
    }

    @Test fun errorsAreNormalizedByTypeAndStatus() {
        assertEquals(OrsErrorCategory.TIMEOUT, classifyOrsError(SocketTimeoutException()))
        assertEquals(OrsErrorCategory.NETWORK, classifyOrsError(IOException()))
        assertEquals(OrsErrorCategory.INVALID_RESPONSE, classifyOrsError(JsonSyntaxException("bad")))
        assertEquals(OrsErrorCategory.AUTH, classifyOrsHttpStatus(401))
        assertEquals(OrsErrorCategory.AUTH, classifyOrsHttpStatus(403))
        assertEquals(OrsErrorCategory.RATE_LIMIT, classifyOrsHttpStatus(429))
        assertEquals(OrsErrorCategory.SERVER, classifyOrsHttpStatus(503))
        assertEquals(OrsErrorCategory.UNKNOWN, classifyOrsHttpStatus(400))
        assertEquals(OrsErrorCategory.UNKNOWN, classifyOrsError(IllegalStateException("private")))
    }
}
