package org.nitri.ors.model.route

import kotlinx.serialization.Serializable

@Serializable
data class Summary(val distance: Double, val duration: Double)