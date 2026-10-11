package com.smartmove.ai.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.ui.auth.SmartMoveTextField
import com.smartmove.ai.ui.components.RiskLevel
import com.smartmove.ai.ui.components.SectionTitle
import com.smartmove.ai.ui.components.SmartMoveButton
import com.smartmove.ai.ui.components.SmartMoveCard
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSurface
import com.smartmove.ai.ui.theme.SmartMoveSurfaceVariant
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Report Hazard Screen (Phase 9).
 *
 * Streamlined form to report road obstacles with type selection,
 * severity level, simulated location, and optional description.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportHazardScreen(
    onBackClick: () -> Unit,
    onReportSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedType by remember { mutableStateOf(HazardType.POTHOLE) }
    var selectedSeverity by remember { mutableStateOf(RiskLevel.MEDIUM) }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val simulatedLocation = "Pune - Hinjewadi Road (Phase 1)"
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SmartMoveBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = SmartMoveTheme.spacing.default),
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SmartMoveTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SmartMoveTextPrimary,
                    )
                }

                Text(
                    text = "Report Road Hazard",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = SmartMoveTextPrimary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

            // 1. WHAT DID YOU SEE?
            SectionTitle(
                title = "WHAT DID YOU SEE?",
                subtitle = "Select the type of hazard observed",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HazardType.entries.forEach { type ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = {
                            Text(
                                text = "${type.icon} ${type.title}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
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

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // 2. SEVERITY
            SectionTitle(
                title = "SEVERITY LEVEL",
                subtitle = "How dangerous is this condition for vehicles?",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH).forEach { severity ->
                    val isSelected = selectedSeverity == severity
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSeverity = severity },
                        label = {
                            Text(
                                text = severity.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = severity.color,
                            selectedLabelColor = SmartMoveTextPrimary,
                            containerColor = SmartMoveSurface,
                            labelColor = SmartMoveTextSecondary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // 3. LOCATION
            SectionTitle(
                title = "LOCATION",
                subtitle = "Simulated road coordinate for report",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📍", fontSize = 20.sp)
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Column {
                        Text(
                            text = simulatedLocation,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = SmartMoveTextPrimary,
                        )
                        Text(
                            text = "Detected from current route",
                            style = MaterialTheme.typography.labelSmall,
                            color = SmartMoveTextMuted,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // 4. OPTIONAL DESCRIPTION
            SectionTitle(
                title = "DESCRIPTION (OPTIONAL)",
                subtitle = "Help other drivers with specific details",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            SmartMoveTextField(
                value = description,
                onValueChange = {
                    if (it.length <= 150) description = it
                },
                label = "e.g. Deep pothole near left lane, slow down",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.huge))

            // 5. SUBMIT BUTTON
            SmartMoveButton(
                text = "SUBMIT REPORT",
                loading = isSubmitting,
                onClick = {
                    isSubmitting = true
                    coroutineScope.launch {
                        HazardRepository.reportHazardRemote(
                            context = context,
                            type = selectedType,
                            severity = selectedSeverity,
                            locationName = simulatedLocation,
                            description = description,
                        )

                        snackbarHostState.showSnackbar("Thanks for helping improve road safety.")
                        delay(600)
                        isSubmitting = false
                        onReportSubmitted()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.huge))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}
