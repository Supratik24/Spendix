package com.smartspend.ai.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.smartspend.ai.R
import kotlinx.coroutines.launch

// IMPORTANT: Replace this with your actual Web Client ID from Google Cloud Console
private const val WEB_CLIENT_ID = "493573007321-mu9pr548qvurtq7t7ufdqmc7m217p7on.apps.googleusercontent.com"

@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_spendix_logo_splash),
            contentDescription = "App Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to Spendix",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Make Every Spend Count",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                if (WEB_CLIENT_ID == "YOUR_WEB_CLIENT_ID_HERE") {
                    errorMessage = "Developer: Please configure your WEB_CLIENT_ID in LoginScreen.kt"
                    return@Button
                }
                
                isLoading = true
                errorMessage = null
                
                coroutineScope.launch {
                    try {
                        val credentialManager = CredentialManager.create(context)
                        
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(WEB_CLIENT_ID)
                            .setAutoSelectEnabled(true)
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        val result = credentialManager.getCredential(
                            request = request,
                            context = context
                        )

                        val credential = result.credential
                        if (credential is androidx.credentials.CustomCredential &&
                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val email = googleIdTokenCredential.id
                            val name = googleIdTokenCredential.displayName ?: email
                            onLoginSuccess(email, name)
                        } else {
                            errorMessage = "Unexpected credential type."
                            isLoading = false
                        }
                    } catch (e: GetCredentialException) {
                        Log.e("Auth", "Login failed", e)
                        errorMessage = "Sign in failed: ${e.message}"
                        isLoading = false
                    } catch (e: Exception) {
                        Log.e("Auth", "Unknown error", e)
                        errorMessage = "An unexpected error occurred."
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Continue with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
