package com.example.usbfelhr

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityViewModel:ViewModel() {
    val _seconds = MutableLiveData<String>()
    private val _finished = MutableLiveData<Boolean>()

    // getter method for seconds var
    fun seconds(): LiveData<String> {
        return _seconds
    }

    // getter method for finished var
    fun finished():LiveData<Boolean>{
        return _finished
    }
}