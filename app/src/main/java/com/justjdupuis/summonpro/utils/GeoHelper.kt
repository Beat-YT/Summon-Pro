package com.justjdupuis.summonpro.utils

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil

object GeoHelper {
    data class ClampedResult(
        val segmentStart: LatLng,
        val segmentEnd: LatLng,
        val clampedPoint: LatLng,
        val segmentIndex: Int
    )

    fun clampToCircle(center: LatLng, target: LatLng, radiusMeters: Double): LatLng {
        val distance = SphericalUtil.computeDistanceBetween(center, target)

        return if (distance <= radiusMeters) {
            target
        } else {
            val heading = SphericalUtil.computeHeading(center, target)
            SphericalUtil.computeOffset(center, radiusMeters, heading)
        }
    }

    fun clampToSegmentedPolyline(point: LatLng, pathPoints: List<LatLng>): ClampedResult? {
        if (pathPoints.size < 2) return null

        var minDistance = Double.MAX_VALUE
        var closestResult: ClampedResult? = null

        for (i in 0 until pathPoints.size - 1) {
            val start = pathPoints[i]
            val end = pathPoints[i + 1]

            val projected = projectPointOnSegment(point, start, end)
            val distance = SphericalUtil.computeDistanceBetween(point, projected)

            if (distance < minDistance) {
                minDistance = distance
                closestResult = ClampedResult(
                    segmentStart = start,
                    segmentEnd = end,
                    clampedPoint = projected,
                    segmentIndex = i
                )
            }
        }

        return closestResult
    }

    private fun projectPointOnSegment(p: LatLng, a: LatLng, b: LatLng): LatLng {
        val headingAB = SphericalUtil.computeHeading(a, b)
        val headingAP = SphericalUtil.computeHeading(a, p)

        val distanceAP = SphericalUtil.computeDistanceBetween(a, p)
        val angle = Math.toRadians(headingAP - headingAB)

        val projDistance = distanceAP * Math.cos(angle)
        val clampedDistance = projDistance.coerceIn(0.0, SphericalUtil.computeDistanceBetween(a, b))

        return SphericalUtil.computeOffset(a, clampedDistance, headingAB)
    }


}