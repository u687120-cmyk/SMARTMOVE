package com.smartmove.ai.ui.alerts

import androidx.compose.runtime.mutableStateListOf
import com.smartmove.ai.ui.components.RiskLevel

/**
 * In-Memory Central Repository for Road Hazards.
 *
 * Serves as the Single Source of Truth for Home, Active Trip, and Alerts screens.
 * Implements deterministic hazard lifecycle evaluation and community confirmation algorithms.
 */
object HazardRepository {

    private val _hazards = mutableStateListOf<HazardItem>()

    init {
        seedInitialHazards()
    }

    private fun seedInitialHazards() {
        if (_hazards.isEmpty()) {
            _hazards.addAll(
                listOf(
                    HazardItem(
                        id = "hz-001",
                        type = HazardType.ACCIDENT,
                        title = "Accident reported",
                        severity = RiskLevel.HIGH,
                        locationName = "Pune - Hinjewadi Bypass (Near Flyover)",
                        distanceMeters = 500,
                        description = "Multi-vehicle collision on right lane. Expect significant delays.",
                        confidence = 92,
                        status = HazardStatus.ACTIVE,
                        reportedAtDisplay = "8 min ago",
                        lastConfirmedAtDisplay = "2 min ago",
                        reportCount = 4,
                        confirmationCount = 7,
                        fixedReportCount = 0,
                    ),
                    HazardItem(
                        id = "hz-002",
                        type = HazardType.POTHOLE,
                        title = "Pothole detected",
                        severity = RiskLevel.MEDIUM,
                        locationName = "Pune - Hinjewadi Road (Phase 1 Entry)",
                        distanceMeters = 300,
                        description = "Deep crater across middle lane. Vehicles braking abruptly.",
                        confidence = 87,
                        status = HazardStatus.ACTIVE,
                        reportedAtDisplay = "12 min ago",
                        lastConfirmedAtDisplay = "4 min ago",
                        reportCount = 2,
                        confirmationCount = 5,
                        fixedReportCount = 0,
                    ),
                    HazardItem(
                        id = "hz-003",
                        type = HazardType.CONSTRUCTION,
                        title = "Road work underway",
                        severity = RiskLevel.LOW,
                        locationName = "Wakad Bridge Underpass",
                        distanceMeters = 1200,
                        description = "Road surfacing and lane narrowing on left shoulder.",
                        confidence = 75,
                        status = HazardStatus.ACTIVE,
                        reportedAtDisplay = "25 min ago",
                        lastConfirmedAtDisplay = "15 min ago",
                        reportCount = 1,
                        confirmationCount = 3,
                        fixedReportCount = 0,
                    ),
                    HazardItem(
                        id = "hz-004",
                        type = HazardType.ROAD_BLOCKAGE,
                        title = "Fallen tree branch",
                        severity = RiskLevel.LOW,
                        locationName = "Baner Road",
                        distanceMeters = 2400,
                        description = "Obstruction cleared by municipal teams.",
                        confidence = 10,
                        status = HazardStatus.RESOLVED,
                        reportedAtDisplay = "3 hours ago",
                        lastConfirmedAtDisplay = "1 hour ago",
                        reportCount = 1,
                        confirmationCount = 1,
                        fixedReportCount = 3,
                        isReportedByCurrentUser = true,
                    ),
                )
            )
        }
    }

    /**
     * Retrieves active alerts sorted strictly by priority:
     * 1. HIGH
     * 2. MEDIUM
     * 3. LOW
     * Resolved hazards are excluded from active broadcasts.
     */
    fun getActiveHazards(): List<HazardItem> {
        return _hazards
            .filter { it.status == HazardStatus.ACTIVE || it.status == HazardStatus.UNDER_REVIEW }
            .sortedWith(
                compareByDescending<HazardItem> {
                    when (it.severity) {
                        RiskLevel.HIGH -> 3
                        RiskLevel.MEDIUM -> 2
                        RiskLevel.LOW -> 1
                    }
                }.thenByDescending { it.confidence }
            )
    }

    /**
     * Retrieves all hazards (active, under review, and resolved) for historical audit.
     */
    fun getAllHazards(): List<HazardItem> = _hazards.toList()

    /**
     * Retrieves hazards submitted by the current user.
     */
    fun getMyReports(): List<HazardItem> {
        return _hazards.filter { it.isReportedByCurrentUser }
    }

    fun getHazardById(id: String): HazardItem? {
        return _hazards.find { it.id == id }
    }

    /**
     * Submits a new user-reported hazard.
     * Initial confidence defaults to 60%, status is ACTIVE.
     */
    fun reportHazard(
        type: HazardType,
        severity: RiskLevel,
        locationName: String = "Pune - Hinjewadi Road",
        description: String? = null,
    ): HazardItem {
        val newId = "hz-${System.currentTimeMillis() % 10000}"
        val newHazard = HazardItem(
            id = newId,
            type = type,
            title = "${type.title} reported",
            severity = severity,
            locationName = locationName,
            distanceMeters = 300,
            description = description?.takeIf { it.isNotBlank() },
            confidence = 60,
            status = HazardStatus.ACTIVE,
            reportedAtDisplay = "Just now",
            lastConfirmedAtDisplay = "Just now",
            reportCount = 1,
            confirmationCount = 1,
            fixedReportCount = 0,
            isReportedByCurrentUser = true,
        )

        _hazards.add(0, newHazard)
        return newHazard
    }

    /**
     * Community Feedback: User confirms "YES, STILL THERE".
     * Confidence increases (+10%), capped at 100%.
     */
    fun confirmHazardStillPresent(id: String): HazardItem? {
        val index = _hazards.indexOfFirst { it.id == id }
        if (index == -1) return null

        val current = _hazards[index]
        val newConfidence = (current.confidence + 10).coerceAtMost(100)
        val newStatus = if (newConfidence >= 50) HazardStatus.ACTIVE else HazardStatus.UNDER_REVIEW

        val updated = current.copy(
            confidence = newConfidence,
            confirmationCount = current.confirmationCount + 1,
            lastConfirmedAtDisplay = "Just now",
            status = newStatus,
        )

        _hazards[index] = updated
        return updated
    }

    /**
     * Community Feedback: User reports "FIXED / NO LONGER THERE".
     * Confidence decreases (-20%), clamped at 0%.
     * When fixedCount >= 2 or confidence <= 20%, status transitions to RESOLVED.
     */
    fun reportHazardFixed(id: String): HazardItem? {
        val index = _hazards.indexOfFirst { it.id == id }
        if (index == -1) return null

        val current = _hazards[index]
        val newFixedCount = current.fixedReportCount + 1
        val newConfidence = (current.confidence - 20).coerceAtLeast(0)

        val newStatus = when {
            newFixedCount >= 2 || newConfidence <= 20 -> HazardStatus.RESOLVED
            newConfidence < 50 -> HazardStatus.UNDER_REVIEW
            else -> HazardStatus.ACTIVE
        }

        val updated = current.copy(
            confidence = newConfidence,
            fixedReportCount = newFixedCount,
            lastConfirmedAtDisplay = "Just now",
            status = newStatus,
        )

        _hazards[index] = updated
        return updated
    }

    /**
     * Remote synchronization with Node.js/MySQL Hazard API.
     */
    suspend fun syncWithBackend(context: android.content.Context) {
        try {
            val api = com.smartmove.ai.data.api.ApiClient.getHazardApi(context)
            val response = api.getActiveHazards()
            if (response.isSuccessful && response.body()?.data != null) {
                val dtoList = response.body()!!.data!!
                if (dtoList.isNotEmpty()) {
                    val remoteItems = dtoList.map { dto ->
                        val hType = try {
                            HazardType.valueOf(dto.type.uppercase())
                        } catch (e: Exception) {
                            HazardType.OTHER
                        }
                        val hSev = when (dto.severity.uppercase()) {
                            "HIGH" -> RiskLevel.HIGH
                            "LOW" -> RiskLevel.LOW
                            else -> RiskLevel.MEDIUM
                        }
                        val hStatus = when (dto.status.uppercase()) {
                            "RESOLVED" -> HazardStatus.RESOLVED
                            "UNDER_REVIEW" -> HazardStatus.UNDER_REVIEW
                            else -> HazardStatus.ACTIVE
                        }
                        HazardItem(
                            id = dto.id,
                            type = hType,
                            title = "${hType.title} detected",
                            severity = hSev,
                            locationName = dto.locationName,
                            latitude = dto.latitude,
                            longitude = dto.longitude,
                            distanceMeters = 300,
                            description = dto.description,
                            confidence = dto.confidence,
                            status = hStatus,
                            reportedAtDisplay = dto.reportedAt.take(16).replace("T", " "),
                            lastConfirmedAtDisplay = dto.lastConfirmedAt.take(16).replace("T", " "),
                        )
                    }

                    // Preserve local user-reported items that may not be synced yet
                    val localOnly = _hazards.filter { it.isReportedByCurrentUser && remoteItems.none { r -> r.id == it.id } }
                    _hazards.clear()
                    _hazards.addAll(localOnly)
                    _hazards.addAll(remoteItems)
                }
            }
        } catch (e: Exception) {
            // Keep existing in-memory state on network disconnect
        }
    }

    suspend fun reportHazardRemote(
        context: android.content.Context,
        type: HazardType,
        severity: RiskLevel,
        locationName: String,
        description: String?,
    ): HazardItem {
        // Optimistic local add
        val local = reportHazard(type, severity, locationName, description)
        try {
            val api = com.smartmove.ai.data.api.ApiClient.getHazardApi(context)
            val req = com.smartmove.ai.data.model.ReportHazardRequest(
                type = type.name,
                latitude = local.latitude,
                longitude = local.longitude,
                locationName = locationName,
                severity = severity.name,
                description = description,
            )
            val resp = api.reportHazard(req)
            if (resp.isSuccessful && resp.body()?.data != null) {
                val dto = resp.body()!!.data!!
                val updated = local.copy(id = dto.id)
                val idx = _hazards.indexOfFirst { it.id == local.id }
                if (idx != -1) {
                    _hazards[idx] = updated
                }
                return updated
            }
        } catch (e: Exception) {
            // Network failure fallback
        }
        return local
    }

    suspend fun confirmHazardRemote(
        context: android.content.Context,
        hazardId: String,
        isStillThere: Boolean,
    ): HazardItem? {
        val updated = if (isStillThere) confirmHazardStillPresent(hazardId) else reportHazardFixed(hazardId)
        try {
            val api = com.smartmove.ai.data.api.ApiClient.getHazardApi(context)
            if (isStillThere) {
                api.confirmHazard(hazardId)
            } else {
                api.reportFixed(hazardId)
            }
        } catch (e: Exception) {
            // Network failure fallback
        }
        return updated
    }
}
