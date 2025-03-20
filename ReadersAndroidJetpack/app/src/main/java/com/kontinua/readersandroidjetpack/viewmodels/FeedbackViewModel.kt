package com.kontinua.readersandroidjetpack.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for handling user feedback functionality.
 *
 * Manages:
 * - Dialog visibility state
 * - Feedback text input
 * - Feedback submission logic
 */
class FeedbackViewModel : ViewModel() {

    companion object {
        private const val TAG = "FeedbackViewModel"
    }

    // State for showing/hiding the feedback dialog
    private val _isShowingFeedbackForm = MutableStateFlow(false)
    val feedbackVisibility: StateFlow<Boolean> = _isShowingFeedbackForm.asStateFlow()

    // State to track feedback text
    private val _feedbackText = MutableStateFlow("")
    val feedbackText: StateFlow<String> = _feedbackText.asStateFlow()

    // State to track user email
    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    /**
     * Shows the feedback dialog
     */
    fun showFeedbackForm() {
        _isShowingFeedbackForm.value = true
    }

    /**
     * Hides the feedback dialog and clears input
     */
    fun dismissFeedbackForm() {
        _isShowingFeedbackForm.value = false
        // Clear the feedback text when dialog is dismissed
        _feedbackText.value = ""
        _userEmail.value = ""
    }

    /**
     * Updates user email as user types
     */
    fun updateUserEmail(email: String) {
        _userEmail.value = email
    }

    /**
     * Updates feedback text as user types
     */
    fun updateFeedbackText(text: String) {
        _feedbackText.value = text
    }

    /**
     * Submits feedback to the backend
     * - TODO: Replace with actual API call
     * - TODO: Handle errors
     * - TODO: Clear input after successful submission
     * - TODO: Show success message
     * - TODO: Validate email
     */
    fun submitFeedback() {
        val email = _userEmail.value
        val feedback = _feedbackText.value
        if (feedback.isBlank() || email.isBlank()) return
        viewModelScope.launch {
            try {
                // Example: apiService.submitFeedback(feedback)
                Log.d(TAG, "Feedback submitted from: $email with text: $feedback")

                // Reset state after successful submission
                _userEmail.value = ""
                _feedbackText.value = ""
                _isShowingFeedbackForm.value = false
            } catch (e: Exception) {
                // Handle error (in a real app, you might want to show an error message)
                Log.e(TAG, "Error submitting feedback", e)
            }
        }
    }


}