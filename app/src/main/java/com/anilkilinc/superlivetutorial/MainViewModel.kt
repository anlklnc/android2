package com.anilkilinc.superlivetutorial

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    // Create a LiveData with a String

    val currentName: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun update() {
        currentName.value = "xxx"
    }
}