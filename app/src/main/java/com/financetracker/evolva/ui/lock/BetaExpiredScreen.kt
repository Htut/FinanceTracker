package com.financetracker.evolva.ui.lock

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.R
import com.financetracker.evolva.ui.theme.FinanceColors
import kotlin.system.exitProcess

@Composable
fun BetaExpiredScreen() {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = appName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(20.dp))
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            stringResource(R.string.beta_expired_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = FinanceColors.Text
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            stringResource(R.string.beta_expired_message),
            fontSize = 14.sp,
            color = FinanceColors.TextSoft,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = {
                (context as? Activity)?.finishAffinity()
                exitProcess(0)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = FinanceColors.Accent)
        ) {
            Text(stringResource(R.string.action_exit))
        }
    }
}
