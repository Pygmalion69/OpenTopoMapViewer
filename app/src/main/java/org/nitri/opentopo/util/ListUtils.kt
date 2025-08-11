package org.nitri.opentopo.util

inline fun <T, K> List<T>.distinctByConsecutive(selector: (T) -> K?): List<T> {
    if (isEmpty()) return emptyList()
    val result = mutableListOf<T>()
    var lastKey: K? = null
    for (item in this) {
        val key = selector(item)
        if (key != lastKey) {
            result.add(item)
            lastKey = key
        }
    }
    return result
}