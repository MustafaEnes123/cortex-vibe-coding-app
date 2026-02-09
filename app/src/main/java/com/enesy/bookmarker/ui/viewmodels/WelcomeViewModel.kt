package com.enesy.bookmarker.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enesy.bookmarker.data.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WelcomeViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _signInState = MutableStateFlow<Result<FirebaseUser?>?>(null)
    val signInState = _signInState.asStateFlow()

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            try {
                val user = authRepository.signInWithGoogle(context)
                _signInState.value = Result.success(user)
            } catch (e: Exception) {
                _signInState.value = Result.failure(e)
            }
        }
    }

    fun finishOnboarding() {
        // Here you can save the onboarding state
    }

    fun resetSignInState() {
        _signInState.value = null
    }
}
