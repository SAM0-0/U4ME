package com.example.vehicledata

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ==========================================
// APPLICATION
// ==========================================
class U4MeApp : Application() {
    companion object {
        lateinit var authRepository: AuthRepository
    }

    override fun onCreate() {
        super.onCreate()
        authRepository = AuthRepository(this)
    }
}

// ==========================================
// MAIN ACTIVITY
// ==========================================
class MainActivity : ComponentActivity() {
    private val viewModel: VehicleViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VehicleDataTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen(viewModel, authViewModel)
                }
            }
        }
    }
}

// ==========================================
// AUTHENTICATION STATE & VIEWMODEL
// ==========================================
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object MobileEntered : AuthUiState()
    object OtpSent : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepo = U4MeApp.authRepository

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    var mobileNumber = ""
        private set

    init {
        if (authRepo.isLoggedIn()) {
            _uiState.value = AuthUiState.Success
        }
    }

    fun submitCredentials(rawMobile: String, rawPass: String) {
        val mobile = rawMobile.trim()
        val pass = rawPass.trim()
        
        mobileNumber = mobile
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                // First call appLogin
                val appLoginResponse = RetrofitInstance.api.appLogin(mobile)
                if (appLoginResponse.statusCode != 200) {
                    _uiState.value = AuthUiState.Error("Mobile number not registered")
                    return@launch
                }

                // Then call login
                val response = RetrofitInstance.api.login(username = mobile, password = pass)
                val amznToken = response.headers()["X-Amzn-Remapped-Authorization"]
                
                if (response.isSuccessful && amznToken != null) {
                    authRepo.mfaToken = amznToken.replace("Bearer ", "")
                    
                    val otpResponse = RetrofitInstance.api.generateOtp()
                    if (otpResponse.status == "SUCCESS") {
                        _uiState.value = AuthUiState.OtpSent
                    } else {
                        _uiState.value = AuthUiState.Error(otpResponse.message ?: "Failed to send OTP")
                    }
                } else {
                    val errorString = if (response.isSuccessful) {
                        response.body()?.string()
                    } else {
                        response.errorBody()?.string()
                    }
                    handleLoginError(errorString)
                }
            } catch (e: retrofit2.HttpException) {
                val errorString = e.response()?.errorBody()?.string()
                handleLoginError(errorString ?: e.message())
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Network error")
            }
        }
    }

    private fun handleLoginError(errorString: String?) {
        var errorMsg = "Invalid credentials or mapping failed"
        if (!errorString.isNullOrEmpty()) {
            try {
                val json = org.json.JSONObject(errorString)
                if (json.has("message")) {
                    val msg = json.getString("message")
                    if (msg.contains("AE1015")) {
                        errorMsg = "Invalid mobile number or password."
                    } else if (msg.contains("AE1013")) {
                        errorMsg = "Your account has been locked due to too many failed attempts."
                    } else if (msg.contains("AE1016")) {
                        errorMsg = "Your device ID has been blocked. Generating a new one, please try again."
                        authRepo.clearAppUniqueId()
                    } else {
                        errorMsg = msg
                    }
                }
            } catch (e: Exception) {
                errorMsg = "Parse Error: ${e.message} | Raw: ${errorString.take(100)}"
            }
        } else {
            errorMsg = "Error body was empty or null"
        }
        _uiState.value = AuthUiState.Error(errorMsg)
    }

    fun submitOtp(otp: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.validateOtp(otp)
                val jwt = response.headers()["Jwttoken"]
                val refresh = response.headers()["Refreshtoken"]
                
                if (jwt != null) {
                    authRepo.jwtToken = jwt.replace("Bearer ", "")
                    authRepo.refreshToken = refresh
                    authRepo.mfaToken = null 
                    _uiState.value = AuthUiState.Success
                } else {
                    _uiState.value = AuthUiState.Error("Invalid OTP")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Network error")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun logout() {
        authRepo.clearTokens()
        _uiState.value = AuthUiState.Idle
    }
}

// ==========================================
// DASHBOARD STATE & VIEWMODEL
// ==========================================
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: VehicleDataModel) : UiState()
    data class Error(val message: String) : UiState()
}

class VehicleViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _likesCount = MutableStateFlow(0)
    val likesCount: StateFlow<Int> = _likesCount

    private val _isLiking = MutableStateFlow(false)
    val isLiking: StateFlow<Boolean> = _isLiking

    private val _hasLiked = MutableStateFlow(prefs.getBoolean("has_liked", false))
    val hasLiked: StateFlow<Boolean> = _hasLiked

    private val _myInfo = MutableStateFlow<MyInfoData?>(null)
    val myInfo: StateFlow<MyInfoData?> = _myInfo

    init {
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = RetrofitInstance.api.fetchVehicleData()
                response.dataModel?.let {
                    _uiState.value = UiState.Success(it)
                } ?: run {
                    _uiState.value = UiState.Error("No data found")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    fun fetchMyInfo() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.myInfo()
                if (response.status == "Success") {
                    _myInfo.value = response.data
                }
            } catch (e: Exception) {
                // Handle silently
            }
        }
    }
    
    fun postLike() {
        if (_hasLiked.value || _isLiking.value) return
        viewModelScope.launch {
            _isLiking.value = true
            try {
                val response = RetrofitInstance.api.sendLike()
                if (response.success) {
                    _likesCount.value = response.likes
                    _hasLiked.value = true
                    prefs.edit().putBoolean("has_liked", true).apply()
                }
            } catch (e: Exception) {
                // Ignore network errors for likes
            } finally {
                _isLiking.value = false
            }
        }
    }
}
