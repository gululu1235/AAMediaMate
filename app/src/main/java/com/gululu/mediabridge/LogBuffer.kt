// LogBuffer.kt
package com.gululu.mediabridge

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object LogBuffer {
    private val _logs = MutableLiveData<String>("")
    val logs: LiveData<String> = _logs

    fun append(line: String) {
        val current = _logs.value ?: ""
        _logs.postValue(current + "\n" + line)
    }
}
