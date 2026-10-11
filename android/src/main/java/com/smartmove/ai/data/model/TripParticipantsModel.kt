package com.smartmove.ai.data.model

/**
 * SMARTMOVE AI — Trip Participant ("Who is Traveling?") Data Model.
 */
enum class ParticipantRole {
    DRIVER,
    PASSENGER
}

data class TripParticipant(
    val id: String,
    val name: String,
    val role: ParticipantRole = ParticipantRole.PASSENGER,
    val isSelected: Boolean = true,
    val phone: String? = null,
    val relation: String = "Traveler"
)
