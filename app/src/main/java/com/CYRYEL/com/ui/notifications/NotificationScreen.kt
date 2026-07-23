package com.CYRYEL.com.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Debug Notificaciones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atras")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshLogs() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    DebugCard(
                        title = "Usuario",
                        content = uiState.userId.ifBlank { "No autenticado" },
                        color = if (uiState.userId.isNotBlank()) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }

                item {
                    DebugCard(
                        title = "FCM Token (generado)",
                        content = uiState.fcmToken,
                        color = if (uiState.fcmToken.startsWith("E")) Color(0xFFC62828) else Color(0xFF1565C0),
                        mono = true
                    )
                }

                item {
                    val saved = uiState.tokenSavedInFirestore
                    DebugCard(
                        title = "Token en Firestore",
                        content = when {
                            saved == null -> "Verificando..."
                            saved -> "CONFIRMADO - Token guardado correctamente"
                            else -> "NO ENCONTRADO - El token no existe en Firestore"
                        },
                        color = when {
                            saved == null -> Color(0xFFF57F17)
                            saved -> Color(0xFF2E7D32)
                            else -> Color(0xFFC62828)
                        }
                    )
                }

                if (uiState.errorMessage != null) {
                    item {
                        DebugCard(
                            title = "Error al obtener token local",
                            content = uiState.errorMessage!!,
                            color = Color(0xFFC62828),
                            mono = true
                        )
                    }
                }

                if (uiState.firestoreToken.isNotBlank()) {
                    item {
                        DebugCard(
                            title = "Token en Firestore (valor)",
                            content = uiState.firestoreToken,
                            color = Color(0xFF1565C0),
                            mono = true
                        )
                    }
                }

                item {
                    val localToken = uiState.fcmToken
                    val remoteToken = uiState.firestoreToken
                    val bothValid = !localToken.startsWith("Error") && remoteToken.isNotBlank() && remoteToken != "No encontrado"
                    val match = bothValid && localToken == remoteToken
                    val statusColor = when {
                        !bothValid -> Color(0xFFF57F17)
                        match -> Color(0xFF2E7D32)
                        else -> Color(0xFFC62828)
                    }
                    DebugCard(
                        title = "Coincidencia tokens",
                        content = when {
                            !bothValid -> "No se puede verificar (token local o remoto no disponible)"
                            match -> "COINCIDEN - Mismo token en dispositivo y Firestore"
                            else -> "NO COINCIDEN - Token local y Firestore son diferentes"
                        },
                        color = statusColor
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Log de FCM (${uiState.logs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.logs.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.clearLogs() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFC62828)
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Limpiar", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (uiState.logs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aun no hay eventos FCM.\n\nAbre la app, ve al inicio, cierrala y envía una notificación de prueba desde Firebase Console.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(uiState.logs.reversed(), key = { it.timestamp }) { entry ->
                    FcmLogItem(entry)
                }
            }
        }
    }
}

@Composable
private fun DebugCard(
    title: String,
    content: String,
    color: Color,
    mono: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                fontSize = if (mono) 11.sp else 14.sp
            )
        }
    }
}

@Composable
private fun FcmLogItem(entry: FcmLogEntry) {
    val bgColor = when (entry.type) {
        FcmLogType.TOKEN_GENERATED -> Color(0xFF1565C0).copy(alpha = 0.1f)
        FcmLogType.TOKEN_SAVED -> Color(0xFF2E7D32).copy(alpha = 0.1f)
        FcmLogType.TOKEN_SAVE_FAILED -> Color(0xFFC62828).copy(alpha = 0.1f)
        FcmLogType.MESSAGE_RECEIVED -> Color(0xFF6A1B9A).copy(alpha = 0.1f)
        FcmLogType.MESSAGE_SHOWN -> Color(0xFF2E7D32).copy(alpha = 0.1f)
        FcmLogType.MESSAGE_DROPPED -> Color(0xFFF57F17).copy(alpha = 0.1f)
        FcmLogType.PERMISSION_CHECK -> Color(0xFF0277BD).copy(alpha = 0.1f)
        FcmLogType.ERROR -> Color(0xFFC62828).copy(alpha = 0.1f)
    }
    val typeLabel = when (entry.type) {
        FcmLogType.TOKEN_GENERATED -> "TOKEN GEN"
        FcmLogType.TOKEN_SAVED -> "TOKEN SAVE"
        FcmLogType.TOKEN_SAVE_FAILED -> "TOKEN FAIL"
        FcmLogType.MESSAGE_RECEIVED -> "MENSAJE"
        FcmLogType.MESSAGE_SHOWN -> "NOTIFICACION"
        FcmLogType.MESSAGE_DROPPED -> "DESCARTADO"
        FcmLogType.PERMISSION_CHECK -> "PERMISO"
        FcmLogType.ERROR -> "ERROR"
    }
    val typeColor = when (entry.type) {
        FcmLogType.MESSAGE_SHOWN, FcmLogType.TOKEN_SAVED -> Color(0xFF2E7D32)
        FcmLogType.MESSAGE_DROPPED, FcmLogType.TOKEN_SAVE_FAILED, FcmLogType.ERROR -> Color(0xFFC62828)
        else -> Color(0xFF1565C0)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.width(80.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = typeColor,
                fontSize = 9.sp
            )
            Text(
                text = entry.formattedTime,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}
