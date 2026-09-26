package app.smartlocker.android

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import app.smartlocker.AppRuntime
import app.smartlocker.SmartLockerApp
import app.smartlocker.platform.AndroidServices
import app.smartlocker.shared.presentation.Route

class MainActivity : ComponentActivity() {
    private val model: LockerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
