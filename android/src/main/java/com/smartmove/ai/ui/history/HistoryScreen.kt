package com.smartmove.ai.ui.history

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.ui.components.SmartMoveButton
import com.smartmove.ai.ui.components.SmartMoveCard
import com.smartmove.ai.ui.components.SmartMoveOutlinedButton
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSurface
import com.smartmove.ai.ui.theme.SmartMoveSurfaceVariant
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme

/**
 * Trip History Screen for SMARTMOVE AI (Phase 6).
 *
 * Implements real date filtering (ALL, TODAY, WEEK, MONTH), LazyColumn trip list,
 * empty state, and transitions to Trip Details.
 */
@Composable
fun HistoryScreen(
    onTripClick: (String) -> Unit,
    onStartTripClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialState: HistoryUiState = HistoryUiState(allTrips = TripHistoryDataProvider.getDemoTrips()),
) {
    var state by remember { mutableStateOf(initialState) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val tripRepo = remember { com.smartmove.ai.data.repository.TripRepository(context) }

    LaunchedEffect(state.selectedFilter) {
        val trips = tripRepo.getTrips(state.selectedFilter)
        state = state.copy(
            allTrips = trips,
            isLoading = false,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SmartMoveBackground)
            .statusBarsPadding()
            .padding(horizontal = SmartMoveTheme.spacing.default),
    ) {
        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

        // Screen Header
        Text(
            text = "Trip History",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = SmartMoveTextPrimary,
        )
        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraSmall))
        Text(
            text = "Your past journeys and safety insights",
            style = MaterialTheme.typography.bodyMedium,
            color = SmartMoveTextSecondary,
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

        // Period Filter Chips: [ ALL ] [ TODAY ] [ WEEK ] [ MONTH ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HistoryFilter.entries.forEach { filter ->
                val isSelected = state.selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { state = state.copy(selectedFilter = filter) },
                    label = {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SmartMovePrimary,
                        selectedLabelColor = SmartMoveTextPrimary,
                        containerColor = SmartMoveSurface,
                        labelColor = SmartMoveTextSecondary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

        // Content Area: Loading, Error, Empty, or List
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = SmartMovePrimary,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp,
                        )
                        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.default))
                        Text(
                            text = "Loading trips...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SmartMoveTextSecondary,
                        )
                    }
                }
            }

            state.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = state.errorMessage ?: "Unable to load trip history.",
                            style = MaterialTheme.typography.titleMedium,
                            color = SmartMoveTextPrimary,
                        )
                        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))
                        SmartMoveOutlinedButton(
                            text = "TRY AGAIN",
                            onClick = {
                                state = state.copy(errorMessage = null)
                            },
                        )
                    }
                }
            }

            state.filteredTrips.isEmpty() -> {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = SmartMoveTheme.spacing.large),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SmartMoveSurfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "🗺️", fontSize = 28.sp)
                        }

                        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

                        Text(
                            text = "No trips found",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = SmartMoveTextPrimary,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

                        Text(
                            text = "You haven't completed any trips during this period.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SmartMoveTextSecondary,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

                        SmartMoveButton(
                            text = "START A TRIP",
                            onClick = onStartTripClick,
                            fullWidth = false,
                        )
                    }
                }
            }

            else -> {
                // Trips LazyColumn
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(SmartMoveTheme.spacing.medium),
                ) {
                    items(
                        items = state.filteredTrips,
                        key = { it.id },
                    ) { trip ->
                        TripHistoryCard(
                            trip = trip,
                            onClick = { onTripClick(trip.id) },
                        )
                    }

                    // Bottom list spacer so content isn't flush with bottom bar
                    item {
                        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.huge))
                    }
                }
            }
        }
    }
}
