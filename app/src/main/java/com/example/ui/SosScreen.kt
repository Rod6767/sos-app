package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.SosState
import com.example.service.SmsDispatcher
import com.example.ui.theme.AmberLocating
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.SosRed
import com.example.ui.theme.SosRedContainer
import com.example.ui.theme.SosRedDark
import com.example.ui.theme.SosRedGlow
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.SosUiState
import com.example.viewmodel.SosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen(
    viewModel: SosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val smsDispatcher = remember { SmsDispatcher(context) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    // Required permissions
    val requiredPermissions = arrayOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val allGranted = permissionsResult.values.all { it }
        if (allGranted) {
            viewModel.initiateEmergencySos(immediate = false)
        } else {
            showPermissionRationale = true
        }
    }

    fun handleSosButtonClick() {
        if (hasAllPermissions()) {
            viewModel.initiateEmergencySos(immediate = false)
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (uiState.sosState is SosState.Idle) SuccessGreen else SosRedGlow)
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Emergency Contact Badge Card
            EmergencyContactBanner(
                contactNumber = uiState.emergencyContact,
                contactName = uiState.contactName,
                onEditClick = { showSettingsDialog = true },
                onCallClick = {
                    context.startActivity(smsDispatcher.createDialerIntent(uiState.emergencyContact))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Massive Central Red SOS Button
            EmergencySosButton(
                state = uiState.sosState,
                countdownSeconds = uiState.countdownRemaining,
                onClick = { handleSosButtonClick() },
                onCancelCountdown = { viewModel.cancelCountdown() }
            )

            // Dynamic Live Status Area
            LiveStatusCard(
                state = uiState.sosState,
                uiState = uiState,
                onReset = { viewModel.resetSosState() },
                onOpenSmsApp = {
                    val msg = uiState.generatedMessage.ifBlank {
                        "EMERGENCY SOS! I need help at: https://maps.google.com"
                    }
                    context.startActivity(smsDispatcher.createSmsIntent(uiState.emergencyContact, msg))
                },
                onOpenMaps = { lat, lng ->
                    context.startActivity(smsDispatcher.createMapsIntent(lat, lng))
                }
            )

            // Safety instructions & quick actions
            SafetyGuidanceCard(
                onDialEmergency = {
                    context.startActivity(smsDispatcher.createDialerIntent("911"))
                }
            )
        }
    }

    if (showSettingsDialog) {
        EmergencyContactDialog(
            initialNumber = uiState.emergencyContact,
            initialName = uiState.contactName,
            countdownEnabled = uiState.autoSendCountdownEnabled,
            onDismiss = { showSettingsDialog = false },
            onSave = { number, name, countdown ->
                viewModel.setEmergencyContact(number, name)
                viewModel.toggleCountdownMode(countdown)
                showSettingsDialog = false
            }
        )
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Permissions Required",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "To send your emergency SOS, this app requires SMS permission to dispatch the message and Location permission to attach your precise Google Maps coordinates.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        permissionLauncher.launch(requiredPermissions)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SosRed)
                ) {
                    Text("Grant Permissions", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Dismiss", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun EmergencySosButton(
    state: SosState,
    countdownSeconds: Int,
    onClick: () -> Unit,
    onCancelCountdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val isTransmitting = state is SosState.AcquiringLocation || state is SosState.SendingSms
    val isCountdown = state is SosState.Countdown

    Box(
        modifier = modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing ripple rings
        Box(
            modifier = Modifier
                .size(if (isTransmitting || isCountdown) 260.dp else 240.dp)
                .scale(if (isTransmitting || isCountdown) pulseScale else 1f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SosRed.copy(alpha = if (isTransmitting) 0.45f else 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Middle ring border
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .border(
                    width = 3.dp,
                    color = if (isCountdown) AmberLocating else SosRedGlow.copy(alpha = 0.8f),
                    shape = CircleShape
                )
        )

        // Main tactile button
        Surface(
            modifier = Modifier
                .size(200.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = SosRedDark,
                    spotColor = SosRedGlow
                )
                .clip(CircleShape)
                .testTag("emergency_sos_button")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White)
                ) {
                    if (isCountdown) {
                        onCancelCountdown()
                    } else if (!isTransmitting) {
                        onClick()
                    }
                },
            color = if (isCountdown) Color(0xFFD84315) else SosRed,
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isCountdown) {
                                listOf(Color(0xFFFF7043), Color(0xFFBF360C))
                            } else {
                                listOf(SosRedGlow, SosRedDark)
                            }
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when {
                        isCountdown -> {
                            Text(
                                text = "$countdownSeconds",
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "TAP TO CANCEL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                                letterSpacing = 1.sp
                            )
                        }
                        isTransmitting -> {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "ALERTING...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.2.sp
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.emergency_sos),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "TAP TO ACTIVATE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyContactBanner(
    contactNumber: String,
    contactName: String,
    onEditClick: () -> Unit,
    onCallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkBorder, DarkBorder)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "EMERGENCY RECIPIENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SosRedGlow,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contactName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = contactNumber,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onCallClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = SuccessGreen
                    ),
                    modifier = Modifier.testTag("call_contact_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call Contact",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontSize = 13.sp)
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(DarkSurfaceVariant, CircleShape)
                        .testTag("edit_contact_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Contact",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveStatusCard(
    state: SosState,
    uiState: SosUiState,
    onReset: () -> Unit,
    onOpenSmsApp: () -> Unit,
    onOpenMaps: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                is SosState.Success -> Color(0xFF102818)
                is SosState.Error -> Color(0xFF2D1214)
                is SosState.Countdown,
                is SosState.AcquiringLocation,
                is SosState.SendingSms -> Color(0xFF281C10)
                else -> DarkSurface
            }
        ),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                when (state) {
                    is SosState.Success -> listOf(SuccessGreen.copy(alpha = 0.5f), SuccessGreen.copy(alpha = 0.2f))
                    is SosState.Error -> listOf(SosRedGlow.copy(alpha = 0.5f), SosRedGlow.copy(alpha = 0.2f))
                    else -> listOf(DarkBorder, DarkBorder)
                }
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (state) {
                is SosState.Idle -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYSTEM ARMED & READY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "When activated, the app will instantly acquire high-precision GPS coordinates and dispatch an SMS alert with a Google Maps location link to ${uiState.emergencyContact}.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }

                is SosState.Countdown -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AmberLocating,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PRE-ALERT COUNTDOWN: ${state.secondsRemaining}s",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberLocating,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Sending emergency SMS to ${uiState.emergencyContact} in ${state.secondsRemaining} seconds. Tap the button or Cancel to abort.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                is SosState.AcquiringLocation -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AmberLocating,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ACQUIRING GPS FIX...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberLocating,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = stringResource(R.string.locating),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                is SosState.SendingSms -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = SosRedGlow,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DISPATCHING SMS ALERT...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SosRedGlow,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Location fixed (${String.format(Locale.US, "%.5f, %.5f", state.latitude, state.longitude)}). Transmitting SMS via cellular network...",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                is SosState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ALERT SENT SUCCESSFULLY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "SMS dispatched to ${state.recipient} at ${
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(state.timestamp))
                        }.",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )

                    if (state.mapsUrl.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenMaps(state.latitude, state.longitude) },
                            color = DarkSurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Google Maps Link Sent:",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = state.mapsUrl,
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text("Reset Status")
                        }
                    }
                }

                is SosState.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = SosRedGlow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DISPATCH ATTENTION NEEDED",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SosRedGlow,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = state.message,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenSmsApp,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_sms_fallback_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SosRed)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send via SMS App", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onReset,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SafetyGuidanceCard(
    onDialEmergency: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkBorder, DarkBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "CRITICAL EMERGENCY SERVICES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )

                FilledTonalButton(
                    onClick = onDialEmergency,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SosRedContainer,
                        contentColor = SosRedGlow
                    ),
                    modifier = Modifier.testTag("dial_911_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dial 911", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Text(
                text = "• Cellular SMS transmits even in low-bandwidth coverage.\n• Your exact GPS coordinates will open directly in Google Maps on the recipient's phone.\n• Keep GPS Location enabled in Android System Settings for maximum accuracy.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun EmergencyContactDialog(
    initialNumber: String,
    initialName: String,
    countdownEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (number: String, name: String, countdown: Boolean) -> Unit
) {
    var number by remember { mutableStateOf(initialNumber) }
    var name by remember { mutableStateOf(initialName) }
    var countdown by remember { mutableStateOf(countdownEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Emergency Settings",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Name / Label") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = SosRed,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = SosRed,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_name_input")
                )

                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Emergency Phone Number") },
                    placeholder = { Text("+1234567890") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = SosRed,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = SosRed,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_number_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "3-Second Cancel Countdown",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Allows canceling accidental button taps",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = countdown,
                        onCheckedChange = { countdown = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SosRed
                        ),
                        modifier = Modifier.testTag("countdown_toggle")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(number, name, countdown) },
                colors = ButtonDefaults.buttonColors(containerColor = SosRed),
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text("Save Configuration", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
