package com.financetracker.evolva.ui.lock

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.data.AppConstants
import com.financetracker.evolva.data.security.DeviceCredentialAuth
import com.financetracker.evolva.ui.theme.FinanceColors

@Composable
fun AppLockScreen(
    onUnlockWithPassword: (String, (Boolean) -> Unit) -> Unit,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val deviceAuthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onUnlocked()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            AppConstants.APP_NAME,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = FinanceColors.Text
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Enter your app password to continue",
            fontSize = 13.sp,
            color = FinanceColors.TextSoft
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                error = null
            },
            label = { Text("App password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = FinanceColors.Expense, fontSize = 12.5.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                onUnlockWithPassword(password) { ok ->
                    if (ok) onUnlocked() else error = "Incorrect password"
                }
            },
            enabled = password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Unlock") }

        val confirmIntent = activity?.let {
            DeviceCredentialAuth.createConfirmIntent(
                it,
                title = "Unlock ${AppConstants.APP_NAME}",
                description = "Confirm with your device lock"
            )
        }
        if (confirmIntent != null) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { deviceAuthLauncher.launch(confirmIntent) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Use device lock") }
        }
    }
}
