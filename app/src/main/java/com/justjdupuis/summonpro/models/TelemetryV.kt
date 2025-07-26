package com.justjdupuis.summonpro.models

data class RelayBase (
    val topic: String,
    val content: Any
)
data class TypeTelemetryV (
    val topic: String,
    val content: TelemetryV
)

data class TelemetryV (
    val data: List<Datum>,
    val createdAt: String,
    val vin: String,
    val isResend: Boolean
)

data class Datum (
    val key: String,
    val value: Value
)

data class Value (
    val locationValue: LocationValue
)

data class LocationValue (
    val latitude: Double,
    val longitude: Double
)
