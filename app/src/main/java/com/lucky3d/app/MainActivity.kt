package com.lucky3d.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lucky3d.app.app.navigation.AppNavigation
import com.lucky3d.app.ui.theme.Lucky3DTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lucky3DTheme {
                Lucky3DApp()
            }
        }
    }
}

@Composable
fun Lucky3DApp() {
    AppNavigation()
}

@Preview(showBackground = true)
@Composable
private fun Lucky3DAppPreview() {
    Lucky3DTheme {
        Lucky3DApp()
    }
}
