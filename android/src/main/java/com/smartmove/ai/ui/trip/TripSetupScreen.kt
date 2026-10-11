package com.smartmove.ai.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.data.api.PlacePredictionDto
import com.smartmove.ai.data.location.LocationProvider
import com.smartmove.ai.data.model.ParticipantRole
import com.smartmove.ai.data.model.TripParticipant
import com.smartmove.ai.data.repository.PlacesRepository
import com.smartmove.ai.data.repository.RouteRepository
import com.smartmove.ai.ui.components.RiskLevel
import com.smartmove.ai.ui.components.SmartMoveButton
import com.smartmove.ai.ui.components.SmartMoveCard
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMoveDanger
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSafe
import com.smartmove.ai.ui.theme.SmartMoveSurface
import com.smartmove.ai.ui.theme.SmartMoveSurfaceVariant
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme
import com.smartmove.ai.ui.theme.SmartMoveWarning
import kotlinx.coroutines.launch

/**
 * SMARTMOVE AI — Interactive Pre-Trip Planner & Setup Screen.
 *
 * Implements GPS location detection, Google Places autocomplete destination search,
 * real route calculation (distance, ETA, safety score), and "Who is Traveling?" selection.
 */
@Composable
fun TripSetupScreen(
    onBackClick: () -> Unit,
    onStartTripWithData: (TripUiState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val placesRepo = remember { PlacesRepository(context) }
    val routeRepo = remember { RouteRepository(context) }

    // State for Location & Destination
    var originAddress by remember { mutableStateOf("Detecting GPS location...") }
    var originLat by remember { mutableStateOf(18.5204) }
    var originLng by remember { mutableStateOf(73.8567) }
    var isLocating by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedDestinationName by remember { mutableStateOf("Hinjewadi Phase 1, Pune") }
    var destLat by remember { mutableStateOf(18.5913) }
    var destLng by remember { mutableStateOf(73.7389) }
    var isSearching by remember { mutableStateOf(false) }
    var searchPredictions by remember { mutableStateOf<List<PlacePredictionDto>>(emptyList()) }

    // State for Route & Safety
    var isCalculatingRoute by remember { mutableStateOf(false) }
    var routeDistanceKm by remember { mutableStateOf(14.7) }
    var routeEtaMinutes by remember { mutableStateOf(32) }
    var routeName by remember { mutableStateOf("Via Paud Road / Hinjewadi Highway") }
    var routeRiskScore by remember { mutableStateOf(24) }
    var routeRiskLevel by remember { mutableStateOf(RiskLevel.LOW) }
    var routeTraffic by remember { mutableStateOf("Moderate") }

    // State for Participants ("Who is Traveling?")
    val participants = remember {
        mutableStateListOf(
            TripParticipant("p-me", "You", ParticipantRole.DRIVER, isSelected = true),
            TripParticipant("p-1", "Rahul", ParticipantRole.PASSENGER, isSelected = true),
            TripParticipant("p-2", "Aman", ParticipantRole.PASSENGER, isSelected = false),
            TripParticipant("p-3", "Mother", ParticipantRole.PASSENGER, isSelected = false),
        )
    }
    var showAddParticipantDialog by remember { mutableStateOf(false) }
    var newPersonName by remember { mutableStateOf("") }

    // Function to calculate Route & Safety
    fun calculateRoute(destName: String, dLat: Double, dLng: Double) {
        scope.launch {
            isCalculatingRoute = true
            try {
                val result = routeRepo.getRecommendedRoutes(
                    originLat = originLat,
                    originLng = originLng,
                    destLat = dLat,
                    destLng = dLng,
                )
                val primary = result.routes.firstOrNull()
                if (primary != null) {
                    routeDistanceKm = primary.distanceKm
                    routeEtaMinutes = primary.durationMinutes
                    routeName = primary.name
                    routeRiskScore = primary.riskScore
                    routeRiskLevel = primary.riskLevel
                    routeTraffic = primary.traffic
                }
            } catch (_: Exception) {}
            isCalculatingRoute = false
        }
    }

    // Initial GPS Location Fetch
    fun refreshLocation() {
        scope.launch {
            isLocating = true
            val loc = LocationProvider.getFreshLocation(context)
            if (loc != null) {
                originLat = loc.latitude
                originLng = loc.longitude
                originAddress = LocationProvider.getAddressName(context, loc.latitude, loc.longitude)
            } else {
                val coords = LocationProvider.getLastKnownCoordinates(context)
                originLat = coords.first
                originLng = coords.second
                originAddress = LocationProvider.getAddressName(context, originLat, originLng)
            }
            isLocating = false
            calculateRoute(selectedDestinationName, destLat, destLng)
        }
    }

    LaunchedEffect(Unit) {
        refreshLocation()
    }

    // Search query watcher
    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().length >= 2) {
            isSearching = true
            searchPredictions = placesRepo.searchPlaces(searchQuery.trim())
            isSearching = false
        } else {
            searchPredictions = emptyList()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SmartMoveBackground)
            .statusBarsPadding()
            .padding(horizontal = SmartMoveTheme.spacing.default)
            .verticalScroll(scrollState),
    ) {
        // 1. Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SmartMoveTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(SmartMoveSurface, CircleShape)
                    .border(1.dp, SmartMoveBorder, CircleShape),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SmartMoveTextPrimary,
                )
            }
            Spacer(modifier = Modifier.width(SmartMoveTheme.spacing.medium))
            Column {
                Text(
                    text = "Plan Your Trip",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SmartMoveTextPrimary,
                )
                Text(
                    text = "Live GPS, Google Routes & Safety Engine",
                    style = MaterialTheme.typography.labelSmall,
                    color = SmartMoveTextMuted,
                )
            }
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

        // 2. Origin & Destination Selector Card
        SmartMoveCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Origin (Current GPS Location)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(SmartMoveSafe.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Current Location",
                        tint = SmartMoveSafe,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(SmartMoveTheme.spacing.small))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CURRENT LOCATION (GPS)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = SmartMoveSafe,
                    )
                    Text(
                        text = originAddress,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = SmartMoveTextPrimary,
                        maxLines = 1,
                    )
                }
                IconButton(
                    onClick = { refreshLocation() },
                    modifier = Modifier.size(32.dp),
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = SmartMovePrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh GPS",
                            tint = SmartMoveTextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

            // Destination Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = if (selectedDestinationName.isNotEmpty()) selectedDestinationName else "Where are you going? (e.g. Hinjewadi)",
                        color = if (selectedDestinationName.isNotEmpty()) SmartMoveTextPrimary else SmartMoveTextMuted,
                        fontSize = 14.sp,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Destination",
                        tint = SmartMovePrimary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = SmartMoveTextMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SmartMovePrimary,
                    unfocusedBorderColor = SmartMoveBorder,
                    focusedContainerColor = SmartMoveSurfaceVariant,
                    unfocusedContainerColor = SmartMoveSurfaceVariant,
                    focusedTextColor = SmartMoveTextPrimary,
                    unfocusedTextColor = SmartMoveTextPrimary,
                ),
                singleLine = true,
            )

            // Autocomplete Suggestions Dropdown
            if (searchPredictions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SmartMoveSurfaceVariant, RoundedCornerShape(8.dp))
                        .border(1.dp, SmartMoveBorder, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                ) {
                    searchPredictions.forEach { pred ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedDestinationName = pred.description
                                    searchQuery = ""
                                    searchPredictions = emptyList()
                                    scope.launch {
                                        val details = placesRepo.getPlaceDetails(pred.placeId)
                                        if (details != null) {
                                            destLat = details.lat
                                            destLng = details.lng
                                        }
                                        calculateRoute(selectedDestinationName, destLat, destLng)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = SmartMovePrimary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = pred.mainText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = SmartMoveTextPrimary,
                                )
                                if (!pred.secondaryText.isNullOrBlank()) {
                                    Text(
                                        text = pred.secondaryText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = SmartMoveTextMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

        // 2.5 Real Interactive Google Map Route Preview
        com.smartmove.ai.ui.map.SmartMoveGoogleMap(
            originLat = originLat,
            originLng = originLng,
            destLat = destLat,
            destLng = destLng,
            originTitle = originAddress,
            destTitle = selectedDestinationName,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

        // 3. Calculated Route & SmartMove Safety Summary Card
        SmartMoveCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ROUTE & SAFETY PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = SmartMoveTextMuted,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(routeRiskLevel.color.copy(alpha = 0.2f))
                        .border(1.dp, routeRiskLevel.color, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "${routeRiskLevel.label} RISK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        ),
                        color = routeRiskLevel.color,
                    )
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            Text(
                text = routeName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SmartMoveTextPrimary,
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Distance",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmartMoveTextMuted,
                    )
                    Text(
                        text = "${routeDistanceKm} km",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SmartMoveTextPrimary,
                    )
                }
                Column {
                    Text(
                        text = "Estimated ETA",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmartMoveTextMuted,
                    )
                    Text(
                        text = "${routeEtaMinutes} min",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SmartMovePrimary,
                    )
                }
                Column {
                    Text(
                        text = "Traffic Condition",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmartMoveTextMuted,
                    )
                    Text(
                        text = routeTraffic,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SmartMoveWarning,
                    )
                }
                Column {
                    Text(
                        text = "Safety Score",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmartMoveTextMuted,
                    )
                    Text(
                        text = "${100 - routeRiskScore}/100",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SmartMoveSafe,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

        // 4. "Who is Traveling?" Section
        SmartMoveCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "WHO IS TRAVELING?",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                        color = SmartMoveTextMuted,
                    )
                    Text(
                        text = "Select trip participants for safety monitoring",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartMoveTextSecondary,
                    )
                }
                IconButton(
                    onClick = { showAddParticipantDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(SmartMovePrimary.copy(alpha = 0.15f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Person",
                        tint = SmartMovePrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            participants.forEachIndexed { index, person ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (person.isSelected) SmartMoveSurfaceVariant else Color.Transparent)
                        .clickable {
                            participants[index] = person.copy(isSelected = !person.isSelected)
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                if (person.isSelected) SmartMovePrimary else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .border(1.dp, if (person.isSelected) SmartMovePrimary else SmartMoveBorder, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (person.isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (person.isSelected) SmartMovePrimary else SmartMoveTextMuted,
                        modifier = Modifier.size(20.dp),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = SmartMoveTextPrimary,
                        modifier = Modifier.weight(1f),
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (person.role == ParticipantRole.DRIVER) SmartMovePrimary.copy(alpha = 0.2f) else SmartMoveBorder.copy(alpha = 0.3f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = if (person.role == ParticipantRole.DRIVER) "DRIVER" else "PASSENGER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (person.role == ParticipantRole.DRIVER) SmartMovePrimary else SmartMoveTextSecondary,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

        // 5. "START TRIP" CTA Button
        SmartMoveButton(
            text = "START TRIP & MONITOR SAFETY",
            onClick = {
                val selectedPeople = participants.filter { it.isSelected }
                val activeState = TripUiState(
                    isTripActive = true,
                    origin = originAddress,
                    destination = selectedDestinationName,
                    currentRoad = routeName,
                    remainingDistanceKm = routeDistanceKm,
                    etaMinutes = routeEtaMinutes,
                    riskScore = routeRiskScore,
                    riskLevel = routeRiskLevel,
                    trafficStatus = routeTraffic,
                    originLat = originLat,
                    originLng = originLng,
                    destLat = destLat,
                    destLng = destLng,
                    participants = selectedPeople.ifEmpty { participants.take(1) },
                )
                onStartTripWithData(activeState)
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))
    }

    // Add Participant Dialog
    if (showAddParticipantDialog) {
        AlertDialog(
            onDismissRequest = { showAddParticipantDialog = false },
            title = {
                Text(text = "Add Trip Participant", color = SmartMoveTextPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Enter the name of the passenger joining this trip:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartMoveTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPersonName,
                        onValueChange = { newPersonName = it },
                        placeholder = { Text("e.g. Aman, Friend, Sister") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPersonName.isNotBlank()) {
                            participants.add(
                                TripParticipant(
                                    id = "p-${System.currentTimeMillis()}",
                                    name = newPersonName.trim(),
                                    role = ParticipantRole.PASSENGER,
                                    isSelected = true,
                                )
                            )
                            newPersonName = ""
                            showAddParticipantDialog = false
                        }
                    }
                ) {
                    Text("Add", color = SmartMovePrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddParticipantDialog = false }) {
                    Text("Cancel", color = SmartMoveTextSecondary)
                }
            }
        )
    }
}
