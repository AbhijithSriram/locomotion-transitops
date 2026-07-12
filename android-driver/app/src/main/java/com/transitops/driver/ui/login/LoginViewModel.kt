package com.transitops.driver.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitops.driver.data.auth.TokenProvider
import com.transitops.driver.data.remote.LoginRequest
import com.transitops.driver.data.remote.NetworkProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // If a token is already cached, skip the login screen entirely.
        if (TokenProvider.isLoggedIn) {
            _uiState.value = LoginUiState.Success
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val response = NetworkProvider.api.login(LoginRequest(email, pass))
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    // Persist all auth data for offline use
                    TokenProvider.token = auth.accessToken
                    TokenProvider.refreshToken = auth.refreshToken
                    TokenProvider.driverId = auth.user.driverId?.toString()
                    TokenProvider.driverName = auth.user.name
                    Log.i("LoginViewModel", "Login OK — ${auth.user.name} (role=${auth.user.role})")
                    _uiState.value = LoginUiState.Success
                } else {
                    val code = response.code()
                    val msg = if (code == 401) "Invalid email or password" else "Login failed ($code)"
                    _uiState.value = LoginUiState.Error(msg)
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login network error", e)
                _uiState.value = LoginUiState.Error("Network error — check your connection")
            }
        }
    }

    fun logout() {
        TokenProvider.clear()
        _uiState.value = LoginUiState.Idle
    }
}
