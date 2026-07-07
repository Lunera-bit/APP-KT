package com.CYRYEL.com.ui.auth

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.CYRYEL.com.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthRoute(
    modifier: Modifier = Modifier,
    onNavigateToMain: (role: String) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.CYRYEL.com.R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("GOOGLE_DEBUG", "Result recibido: código=${result.resultCode}, data=${result.data}")
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            val token = account.idToken
            if (token != null) {
                viewModel.signInWithGoogle(token)
            } else {
                android.util.Log.w("GOOGLE_SIGNIN", "idToken es null")
                viewModel.showError("Error: No se pudo obtener el token de Google")
            }
        } catch (e: ApiException) {
            android.util.Log.e("GOOGLE_SIGNIN", "ApiException codigo=${e.statusCode}: ${e.localizedMessage}", e)
            viewModel.showError("Error de Google (${e.statusCode}): ${e.localizedMessage ?: "desconocido"}")
        }
    }

    LaunchedEffect(uiState.isAuthenticated, uiState.isRoleLoaded) {
        if (uiState.isAuthenticated && uiState.isRoleLoaded) {
            onNavigateToMain(uiState.role)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!uiState.isAuthenticated) {
            LoginScreen(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                onTermsAcceptedChange = viewModel::onTermsAcceptedChange,
                onGoogleSignInClick = {
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                }
            )
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Iniciando sesion...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    uiState: AuthUiState,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CYRYEL STORE",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Inicia sesion para continuar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        ) {
            Checkbox(
                checked = uiState.termsAccepted,
                onCheckedChange = onTermsAcceptedChange
            )
            val uriHandler = LocalUriHandler.current
            val annotatedString = buildAnnotatedString {
                append("Acepto los ")
                pushStringAnnotation("terms", "https://lunera-bit.github.io/CYRYEL/terms-and-conditions.html")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append("Terminos y Condiciones")
                }
                pop()
                append(" y la ")
                pushStringAnnotation("privacy", "https://lunera-bit.github.io/CYRYEL/privacy-policy.html")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append("Politica de Privacidad")
                }
                pop()
            }
            ClickableText(
                text = annotatedString,
                onClick = { offset ->
                    annotatedString.getStringAnnotations(offset, offset).firstOrNull()?.let {
                        uriHandler.openUri(it.item)
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
            )
        }

        Button(
            onClick = onGoogleSignInClick,
            enabled = uiState.termsAccepted && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.google),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Continuar con Google",
                fontWeight = FontWeight.Medium
            )
        }
    }
}
