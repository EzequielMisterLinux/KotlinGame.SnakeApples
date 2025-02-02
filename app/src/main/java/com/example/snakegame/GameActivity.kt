package com.example.snakegame

import android.app.AlertDialog
import android.os.Bundle
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

class GameActivity : AppCompatActivity(), GameCallbacks {
    private lateinit var viewModel: GameViewModel
    private lateinit var snakeGameView: SnakeGameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game_activity)

        viewModel = ViewModelProvider(this).get(GameViewModel::class.java)
        snakeGameView = findViewById(R.id.snakeGameView)
        snakeGameView.setGameCallbacks(this)

        val scoreText = findViewById<TextView>(R.id.scoreText)

        viewModel.score.observe(this) { score ->
            scoreText.text = "Score: $score | Level: ${viewModel.currentLevel}"
        }

        snakeGameView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                snakeGameView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                snakeGameView.resetGame()
            }
        })
    }

    override fun onPause() {
        super.onPause()
        snakeGameView.pause()
    }

    override fun onResume() {
        super.onResume()
        snakeGameView.resume()
    }

    override fun onGameOver(success: Boolean) {
        runOnUiThread {
            showGameOverDialog(success)
        }
    }

    override fun onScoreIncreased(points: Int) {
        runOnUiThread {
            viewModel.increaseScore(points)
        }
    }

    private fun showGameOverDialog(success: Boolean) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(if(success) "Level Completed!" else "Game Over!")
            .setMessage("Score: ${viewModel.score.value}")
            .setPositiveButton("Restart") { _, _ -> restartGame() }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .create()
        dialog.show()
    }

    private fun restartGame() {
        viewModel.resetGame()
        snakeGameView.resetGame()
    }
}