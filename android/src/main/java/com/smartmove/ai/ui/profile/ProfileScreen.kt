package com.smartmove.ai.ui.profile

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.ui.auth.SmartMoveTextField
import com.smartmove.ai.ui.components.SectionTitle
import com.smartmove.ai.ui.components.SmartMoveButton
import com.smartmove.ai.ui.components.SmartMoveCard
import com.smartmove.ai.ui.components.SmartMoveOutlinedButton
import com.smartmove.ai.ui.theme.CardShape
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMoveDanger
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSurface
import com.smartmove.ai.ui.theme.SmartMoveSurfaceVariant
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme
import kotlinx.coroutines.launch

/**
 * SMARTMOVE AI Profile Screen (Phase 5).
 *
 * Implements user identity, vehicle management, local safety preferences,
 * about information, and confirmed logout flow.
 */
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    initialState: ProfileUiState = ProfileUiState(),
) {
    var state by remember { mutableStateOf(initialState) }

    // Dialog & Feedback States
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showEditVehicleDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val userRepo = remember { com.smartmove.ai.data.repository.UserRepository(context) }
    val authRepo = remember { com.smartmove.ai.data.repository.AuthRepository(context) }

    val vehicleCategories = listOf("Car", "Motorcycle", "SUV", "Truck", "Commercial")

    LaunchedEffect(Unit) {
        val pRes = userRepo.getProfile()
        if (pRes.isSuccess) {
            val u = pRes.getOrNull()!!
            state = state.copy(
                userName = u.fullName,
                email = u.email,
                phone = u.phone ?: state.phone,
            )
        }
        val vRes = userRepo.getVehicles()
        if (vRes.isSuccess && vRes.getOrNull()?.isNotEmpty() == true) {
            val v = vRes.getOrNull()!!.first()
            state = state.copy(
                vehicleType = v.vehicleType,
                vehicleNumber = v.vehicleNumber ?: state.vehicleNumber,
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SmartMoveBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(horizontal = SmartMoveTheme.spacing.default, vertical = SmartMoveTheme.spacing.medium),
        ) {
            // Screen Title
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = SmartMoveTextPrimary,
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

            // --------------------------------------------------
            // 1. PROFILE HEADER CARD
            // --------------------------------------------------
            ProfileHeaderCard(
                userName = state.userName,
                email = state.email,
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // --------------------------------------------------
            // 2. PERSONAL INFORMATION
            // --------------------------------------------------
            SectionTitle(
                title = "PERSONAL INFORMATION",
                action = {
                    Text(
                        text = "EDIT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SmartMovePrimary,
                        ),
                        modifier = Modifier
                            .clickable { showEditProfileDialog = true }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                    )
                },
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                InfoDisplayRow(label = "Name", value = state.userName)
                HorizontalItemDivider()
                InfoDisplayRow(label = "Email", value = state.email)
                HorizontalItemDivider()
                InfoDisplayRow(label = "Phone", value = state.phone)
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // --------------------------------------------------
            // 3. VEHICLE INFORMATION
            // --------------------------------------------------
            SectionTitle(
                title = "VEHICLE",
                action = {
                    Text(
                        text = "EDIT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SmartMovePrimary,
                        ),
                        modifier = Modifier
                            .clickable { showEditVehicleDialog = true }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                    )
                },
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                InfoDisplayRow(
                    label = "Vehicle Type",
                    value = "🚗 ${state.vehicleType}",
                )
                HorizontalItemDivider()
                InfoDisplayRow(
                    label = "Vehicle Number",
                    value = "🚘 ${state.vehicleNumber}",
                )
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // --------------------------------------------------
            // 4. SAFETY PREFERENCES
            // --------------------------------------------------
            SectionTitle(
                title = "SAFETY PREFERENCES",
                subtitle = "Local alert & driving configurations",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                // Notifications Switch
                PreferenceSwitchRow(
                    title = "Safety Alerts",
                    subtitle = "Receive dynamic real-time hazard warnings",
                    checked = state.notificationsEnabled,
                    onCheckedChange = { isEnabled ->
                        state = state.copy(notificationsEnabled = isEnabled)
                    },
                )

                HorizontalItemDivider()

                // Location Access Switch
                PreferenceSwitchRow(
                    title = "Location Access",
                    subtitle = "Dynamic risk calculation for current road segment",
                    checked = state.locationEnabled,
                    onCheckedChange = { isEnabled ->
                        state = state.copy(locationEnabled = isEnabled)
                    },
                )

                HorizontalItemDivider()

                // Units Selection Row
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Distance Units",
                        style = MaterialTheme.typography.titleMedium,
                        color = SmartMoveTextPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Selected: ${state.distanceUnit.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmartMoveTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DistanceUnit.entries.forEach { unit ->
                            val isSelected = state.distanceUnit == unit
                            FilterChip(
                                selected = isSelected,
                                onClick = { state = state.copy(distanceUnit = unit) },
                                label = { Text(unit.label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SmartMovePrimary,
                                    selectedLabelColor = SmartMoveTextPrimary,
                                    containerColor = SmartMoveSurfaceVariant,
                                    labelColor = SmartMoveTextSecondary,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // --------------------------------------------------
            // 5. ABOUT SECTION
            // --------------------------------------------------
            SectionTitle(title = "ABOUT")

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                ClickableAboutRow(
                    label = "About SMARTMOVE AI",
                    onClick = { showAboutDialog = true },
                )
                HorizontalItemDivider()
                ClickableAboutRow(
                    label = "Privacy Policy",
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Privacy Policy is currently offline.")
                        }
                    },
                )
                HorizontalItemDivider()
                ClickableAboutRow(
                    label = "Terms of Service",
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Terms of Service is currently offline.")
                        }
                    },
                )
                HorizontalItemDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "App Version",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SmartMoveTextPrimary,
                    )
                    Text(
                        text = state.appVersion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmartMoveTextMuted,
                    )
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.huge))

            // --------------------------------------------------
            // 6. LOG OUT BUTTON
            // --------------------------------------------------
            SmartMoveOutlinedButton(
                text = "LOG OUT",
                onClick = { showLogoutConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.huge))
        }

        // Floating Snackbar for user actions
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }

    // --------------------------------------------------
    // DIALOG 1: EDIT PROFILE
    // --------------------------------------------------
    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(state.userName) }
        var editPhone by remember { mutableStateOf(state.phone) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = SmartMoveSurface,
            shape = CardShape,
            title = {
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SmartMoveTextPrimary,
                )
            },
            text = {
                Column {
                    SmartMoveTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = "Full Name",
                    )
                    Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))
                    SmartMoveTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = "Phone Number",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    )
                    Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))
                    Text(
                        text = "Email cannot be changed: ${state.email}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmartMoveTextMuted,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editName.isNotBlank()) {
                            state = state.copy(
                                userName = editName.trim(),
                                phone = editPhone.trim(),
                            )
                            showEditProfileDialog = false
                            coroutineScope.launch {
                                userRepo.updateProfile(editName.trim(), editPhone.trim())
                                snackbarHostState.showSnackbar("Profile updated")
                            }
                        }
                    }
                ) {
                    Text("SAVE", color = SmartMovePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("CANCEL", color = SmartMoveTextSecondary)
                }
            },
        )
    }

    // --------------------------------------------------
    // DIALOG 2: EDIT VEHICLE
    // --------------------------------------------------
    if (showEditVehicleDialog) {
        var selectedType by remember { mutableStateOf(state.vehicleType) }
        var editNumber by remember { mutableStateOf(state.vehicleNumber) }

        AlertDialog(
            onDismissRequest = { showEditVehicleDialog = false },
            containerColor = SmartMoveSurface,
            shape = CardShape,
            title = {
                Text(
                    text = "Edit Vehicle",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SmartMoveTextPrimary,
                )
            },
            text = {
                Column {
                    Text(
                        text = "Vehicle Category",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SmartMoveTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        vehicleCategories.take(3).forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SmartMovePrimary,
                                    selectedLabelColor = SmartMoveTextPrimary,
                                    containerColor = SmartMoveSurfaceVariant,
                                    labelColor = SmartMoveTextSecondary,
                                ),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        vehicleCategories.drop(3).forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SmartMovePrimary,
                                    selectedLabelColor = SmartMoveTextPrimary,
                                    containerColor = SmartMoveSurfaceVariant,
                                    labelColor = SmartMoveTextSecondary,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

                    SmartMoveTextField(
                        value = editNumber,
                        onValueChange = { editNumber = it },
                        label = "Vehicle Registration / Plate",
                        placeholder = "e.g. MH12AB1234",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        state = state.copy(
                            vehicleType = selectedType,
                            vehicleNumber = editNumber.trim(),
                        )
                        showEditVehicleDialog = false
                        coroutineScope.launch {
                            userRepo.addVehicle(selectedType, editNumber.trim())
                            snackbarHostState.showSnackbar("Vehicle updated")
                        }
                    }
                ) {
                    Text("SAVE", color = SmartMovePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditVehicleDialog = false }) {
                    Text("CANCEL", color = SmartMoveTextSecondary)
                }
            },
        )
    }

    // --------------------------------------------------
    // DIALOG 3: LOGOUT CONFIRMATION
    // --------------------------------------------------
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            containerColor = SmartMoveSurface,
            shape = CardShape,
            title = {
                Text(
                    text = "Log out of SMARTMOVE AI?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SmartMoveTextPrimary,
                )
            },
            text = {
                Text(
                    text = "You will need to sign in again to access your road safety intelligence and driving stats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmartMoveTextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmDialog = false
                        coroutineScope.launch {
                            authRepo.logout()
                            onLogout()
                        }
                    }
                ) {
                    Text("LOG OUT", color = SmartMoveDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("CANCEL", color = SmartMoveTextSecondary)
                }
            },
        )
    }

    // --------------------------------------------------
    // DIALOG 4: ABOUT DIALOG
    // --------------------------------------------------
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = SmartMoveSurface,
            shape = CardShape,
            title = {
                Text(
                    text = "About SMARTMOVE AI",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SmartMoveTextPrimary,
                )
            },
            text = {
                Column {
                    Text(
                        text = "SMARTMOVE AI is a dynamic road-safety intelligence system that estimates the real-time risk of roads based on live conditions, braking telemetry, and environmental factors.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmartMoveTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))
                    Text(
                        text = "Version: ${state.appVersion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmartMoveTextMuted,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", color = SmartMovePrimary, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

/**
 * Circular Avatar + User Name & Email Header Card
 */
@Composable
private fun ProfileHeaderCard(
    userName: String,
    email: String,
    modifier: Modifier = Modifier,
) {
    SmartMoveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = SmartMoveTheme.spacing.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Circular Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SmartMoveSurfaceVariant)
                    .border(2.dp, SmartMovePrimary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile Avatar",
                    tint = SmartMovePrimary,
                    modifier = Modifier.size(44.dp),
                )
            }

            Spacer(modifier = Modifier.width(SmartMoveTheme.spacing.default))

            Column {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = SmartMoveTextPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmartMoveTextSecondary,
                )
            }
        }
    }
}

/**
 * Standard key-value information row.
 */
@Composable
private fun InfoDisplayRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = SmartMoveTextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = SmartMoveTextPrimary,
        )
    }
}

/**
 * Row with title, subtitle, and an accessible Switch control.
 */
@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = SmartMoveTextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SmartMoveTextSecondary,
            )
        }

        Spacer(modifier = Modifier.width(SmartMoveTheme.spacing.medium))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SmartMoveTextPrimary,
                checkedTrackColor = SmartMovePrimary,
                uncheckedThumbColor = SmartMoveTextSecondary,
                uncheckedTrackColor = SmartMoveSurfaceVariant,
                uncheckedBorderColor = SmartMoveBorder,
            ),
        )
    }
}

/**
 * Clickable row for About / Legal links.
 */
@Composable
private fun ClickableAboutRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = SmartMoveTextPrimary,
        )
        Text(
            text = "›",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = SmartMoveTextMuted,
        )
    }
}

/**
 * Subtle separator between rows inside cards.
 */
@Composable
private fun HorizontalItemDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .background(SmartMoveBorder),
    )
}
