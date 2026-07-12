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

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val response = NetworkProvider.api.login(LoginRequest(email, pass))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    TokenProvider.token = authResponse.accessToken
                    Log.i("LoginViewModel", "Login successful for ${authResponse.email} with role ${authResponse.role}")
                    _uiState.value = LoginUiState.Success
                } else {
                    _uiState.value = LoginUiState.Error("Login failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login error", e)
                _uiState.value = LoginUiState.Error("Network error: ${e.message}")
            }
        }
    }
}
