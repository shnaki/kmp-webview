package io.github.shnaki.kmpwebview.bridge.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float
)
