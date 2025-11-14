package com.github.iscle.haven.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoGeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@Serializable
data class GeocodingResult(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    @SerialName("feature_code")
    val featureCode: String? = null,
    @SerialName("country_code")
    val countryCode: String? = null,
    val admin1: String? = null,
    val admin2: String? = null,
    val admin3: String? = null,
    val admin4: String? = null,
    val timezone: String? = null,
    val population: Int? = null,
    @SerialName("country_id")
    val countryId: Int? = null,
    val country: String? = null,
    val postcodes: List<String>? = null
)

