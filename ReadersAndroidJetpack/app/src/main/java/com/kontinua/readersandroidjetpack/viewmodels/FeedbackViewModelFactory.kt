package com.kontinua.readersandroidjetpack.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kontinua.readersandroidjetpack.util.NavbarManager

/**
 * Factory for creating a FeedbackViewModel with the required NavbarManager dependency.
 */
class FeedbackViewModelFactory(private val navbarManager: NavbarManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedbackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedbackViewModel(navbarManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}