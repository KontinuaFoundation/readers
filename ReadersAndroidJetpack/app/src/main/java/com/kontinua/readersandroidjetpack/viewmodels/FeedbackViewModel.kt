package com.kontinua.readersandroidjetpack.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontinua.readersandroidjetpack.util.APIManager
import com.kontinua.readersandroidjetpack.util.NavbarManager
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
class FeedbackViewModel(
    private val navbarManager: NavbarManager
) : ViewModel() {

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

    // State to track submission state
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // State to track submission errors
    private val _submissionError = MutableStateFlow<String?>(null)
    val submissionError: StateFlow<String?> = _submissionError.asStateFlow()

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
     */
    fun submitFeedback() {
        val email = _userEmail.value
        val feedback = _feedbackText.value

        if (feedback.isBlank() || email.isBlank()) {
            return
        }

        val workbookId = navbarManager.collectionVM?.currentWorkbook?.id
        if (workbookId == null) {
            return
        }

        val pageNumber = navbarManager.pageNumber + 1

        val chapter = navbarManager.getCurrentChapter()
        // Default to chapter number 0 if chapter is null (Prefix etc..)
        val chapterNumber = chapter?.chapNum ?: 0

        // Get version information from the collection
        val collection = navbarManager.collectionVM?.collectionState?.value
        if (collection == null) {
            return
        }

        val majorVersion = collection.majorVersion
        val minorVersion = collection.minorVersion
        val localization = collection.localization

        viewModelScope.launch {
            _isSubmitting.value = true
            _submissionError.value = null

            try {
                val success = APIManager.submitFeedback(
                    workbookId = workbookId,
                    chapterNumber = chapterNumber,
                    pageNumber = pageNumber,
                    userEmail = email,
                    description = feedback,
                    majorVersion = majorVersion,
                    minorVersion = minorVersion,
                    localization = localization
                )

                if (success) {
                    // Reset state after successful submission
                    _userEmail.value = ""
                    _feedbackText.value = ""
                    _isShowingFeedbackForm.value = false
                } else {
                    // Handle submission failure
                    _submissionError.value = "Failed to submit feedback. Please try again."
                }
            } catch (e: Exception) {
                // Handle error
                _submissionError.value = "Error: ${e.message}"
            } finally {
                _isSubmitting.value = false
            }
        }
    }


}