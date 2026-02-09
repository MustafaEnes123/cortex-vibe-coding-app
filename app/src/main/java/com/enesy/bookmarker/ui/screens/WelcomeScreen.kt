package com.enesy.bookmarker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enesy.bookmarker.R
import com.enesy.bookmarker.domain.BookmarkerStrings
import com.enesy.bookmarker.ui.viewmodels.WelcomeViewModel
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    strings: BookmarkerStrings,
    viewModel: WelcomeViewModel = viewModel(),
    onNavigateToNext: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val signInState by viewModel.signInState.collectAsState()
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(signInState) {
        signInState?.let { result ->
            if (result.isSuccess) {
                onNavigateToNext()
            } else {
                Toast.makeText(context, "Sign-in failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
            viewModel.resetSignInState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val logoFilter = if (isDark) ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
            -1f,  0f,  0f,  0f, 255f,
            0f, -1f,  0f,  0f, 255f,
            0f,  0f, -1f,  0f, 255f,
            0f,  0f,  0f,  1f,   0f
        ))) else null
        Image(
            painter = painterResource(id = R.drawable.img_cortex_logo),
            contentDescription = "Cortex Logo",
            modifier = Modifier
                .height(120.dp)
                .padding(bottom = 24.dp),
            contentScale = ContentScale.Fit,
            colorFilter = logoFilter
        )

        Text(
            text = strings.welcomeTitle,
            style = MaterialTheme.typography.displayMedium,
            fontSize = 44.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = 52.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(48.dp))

        val buttonColor = if (isDark) Color(0xFF1E1E1E) else Color.White
        val buttonContentColor = if (isDark) Color.White else Color.Black
        val buttonBorder = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null
        val buttonElevation = if (isDark) 0.dp else 6.dp

        Surface(
            onClick = {
                if (signInState?.isSuccess != true) {
                    scope.launch {
                        viewModel.signInWithGoogle(context)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(100),
            color = buttonColor,
            shadowElevation = buttonElevation,
            border = buttonBorder
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (signInState?.isSuccess == true) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = strings.googleSignBtn,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = buttonContentColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = onNavigateToNext
        ) {
            Text(
                text = strings.continueBtn,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
