package com.example.snakegame

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score

    var currentLevel = 1
        private set

    fun increaseScore(points: Int) {
        _score.value = _score.value?.plus(points)
        if(_score.value?.rem(10) == 0) {
            currentLevel++
        }
    }

    fun resetGame() {
        _score.value = 0
        currentLevel = 1
    }
}