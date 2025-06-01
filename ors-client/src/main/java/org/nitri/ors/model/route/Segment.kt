package org.nitri.ors.model.route

import kotlinx.serialization.Serializable

@Serializable
data class Segment(val distance: Double, val duration: Double)