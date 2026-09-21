package org.nitri.opentopo.analytics

import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

enum class OrsOutcome(val value: String) { SUCCESS("success"), EMPTY("empty"), ERROR("error") }
enum class OrsStartSource(val value: String) { CURRENT_LOCATION("current_location"), MARKER_ONLY("marker_only") }
enum class DurationBucket(val value: String) { UNDER_1S("under_1s"), ONE_TO_THREE_SECONDS("1_3s"), OVER_3S("over_3s") }
enum class QueryLengthBucket(val value: String) { THREE_TO_FIVE("3_5"), SIX_TO_TEN("6_10"), OVER_TEN("over_10") }
enum class ResultPositionBucket(val value: String) { FIRST("1"), SECOND_OR_THIRD("2_3"), FOURTH_OR_LATER("4_plus") }
enum class OrsErrorCategory(val value: String) {
    NETWORK("network"), TIMEOUT("timeout"), AUTH("auth"), RATE_LIMIT("rate_limit"),
    SERVER("server"), INVALID_RESPONSE("invalid_response"), UNKNOWN("unknown")
}

fun durationBucket(durationMillis: Long) = when {
    durationMillis < 1_000 -> DurationBucket.UNDER_1S
    durationMillis <= 3_000 -> DurationBucket.ONE_TO_THREE_SECONDS
    else -> DurationBucket.OVER_3S
}

fun queryLengthBucket(length: Int) = when (length) {
    in 3..5 -> QueryLengthBucket.THREE_TO_FIVE
    in 6..10 -> QueryLengthBucket.SIX_TO_TEN
    else -> QueryLengthBucket.OVER_TEN
}

/** [zeroBasedIndex] is intentionally positional; result data never enters analytics. */
fun resultPositionBucket(zeroBasedIndex: Int) = when (zeroBasedIndex) {
    0 -> ResultPositionBucket.FIRST
    1, 2 -> ResultPositionBucket.SECOND_OR_THIRD
    else -> ResultPositionBucket.FOURTH_OR_LATER
}

fun classifyOrsError(error: Throwable): OrsErrorCategory = when (error) {
    is SocketTimeoutException -> OrsErrorCategory.TIMEOUT
    is HttpException -> classifyOrsHttpStatus(error.code())
    is JsonParseException -> OrsErrorCategory.INVALID_RESPONSE
    is IOException -> OrsErrorCategory.NETWORK
    else -> OrsErrorCategory.UNKNOWN
}

fun classifyOrsHttpStatus(status: Int): OrsErrorCategory = when (status) {
    401, 403 -> OrsErrorCategory.AUTH
    429 -> OrsErrorCategory.RATE_LIMIT
    in 500..599 -> OrsErrorCategory.SERVER
    else -> OrsErrorCategory.UNKNOWN
}
