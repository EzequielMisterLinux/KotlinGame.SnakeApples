package com.example.snakegame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.random.Random

class SnakeGameView(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {
    private var thread: Thread? = null
    private var isPlaying = false
    private var isInitialized = false
    private var canvas: Canvas? = null
    private val paint: Paint = Paint()
    private var callbacks: GameCallbacks? = null
    private val handler = Handler(Looper.getMainLooper())

    private var snake = mutableListOf<Point>()
    private var blueApple: Apple? = null
    private var redApple: Apple? = null
    private var direction = Direction.RIGHT
    private var nextDirection = Direction.RIGHT
    private var blockSize = 0
    private var updateDelay = 200L

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (abs(velocityX) > abs(velocityY)) {
                if (velocityX > 0 && direction != Direction.LEFT) {
                    nextDirection = Direction.RIGHT
                    Log.d("SnakeGame", "Swipe RIGHT detected")
                } else if (velocityX < 0 && direction != Direction.RIGHT) {
                    nextDirection = Direction.LEFT
                    Log.d("SnakeGame", "Swipe LEFT detected")
                }
            } else {
                if (velocityY > 0 && direction != Direction.UP) {
                    nextDirection = Direction.DOWN
                    Log.d("SnakeGame", "Swipe DOWN detected")
                } else if (velocityY < 0 && direction != Direction.DOWN) {
                    nextDirection = Direction.UP
                    Log.d("SnakeGame", "Swipe UP detected")
                }
            }
            return true
        }
    })

    init {
        holder.addCallback(this)
        paint.apply {
            color = Color.WHITE
            textSize = 50f
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!isInitialized) {
            initializeGame()
            isInitialized = true
        }
        resume()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (!isInitialized) {
            initializeGame()
            isInitialized = true
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    private fun initializeGame() {
        blockSize = (width / 20).coerceAtLeast(1)

        val centerX = (width / blockSize) / 2
        val centerY = (height / blockSize) / 2

        snake.clear()
        snake.add(Point(centerX - 2, centerY))
        snake.add(Point(centerX - 1, centerY))
        snake.add(Point(centerX, centerY))

        direction = Direction.RIGHT
        nextDirection = Direction.RIGHT

        spawnApples()
    }

    override fun run() {
        while(isPlaying) {
            update()
            draw()
            try {
                Thread.sleep(updateDelay)
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    private fun update() {
        if (!isPlaying || snake.isEmpty()) return

        direction = nextDirection

        val head = snake.last()
        val newHead = when(direction) {
            Direction.UP -> Point(head.x, head.y - 1)
            Direction.DOWN -> Point(head.x, head.y + 1)
            Direction.LEFT -> Point(head.x - 1, head.y)
            Direction.RIGHT -> Point(head.x + 1, head.y)
        }

        if(checkCollision(newHead)) {
            handler.post { callbacks?.onGameOver(false) }
            isPlaying = false
            return
        }

        snake.add(newHead)

        var appleEaten = false

        // Verificar colisión con manzana roja
        redApple?.let {
            if (newHead.x == it.position.x && newHead.y == it.position.y) {
                handler.post { callbacks?.onGameOver(false) }
                isPlaying = false
                return
            }
        }

        // Verificar colisión con manzana azul
        blueApple?.let {
            if (newHead.x == it.position.x && newHead.y == it.position.y) {
                handler.post { callbacks?.onScoreIncreased(1) }
                appleEaten = true
                spawnApples()
            }
        }

        if (!appleEaten) {
            snake.removeAt(0)
        }
    }

    private fun checkCollision(newHead: Point): Boolean {
        if (newHead.x < 0 || newHead.y < 0 ||
            newHead.x >= width/blockSize ||
            newHead.y >= height/blockSize) {
            return true
        }
        return snake.dropLast(1).contains(newHead)
    }

    private fun draw() {
        if (holder.surface.isValid) {
            canvas = holder.lockCanvas()
            canvas?.let { canvas ->
                canvas.drawColor(Color.BLACK)

                // Dibujar serpiente
                snake.forEachIndexed { index, point ->
                    paint.color = if (index == snake.size - 1) {
                        Color.rgb(50, 205, 50)
                    } else {
                        Color.rgb(0, 155, 0)
                    }

                    val padding = blockSize * 0.1f
                    canvas.drawRect(
                        (point.x * blockSize + padding),
                        (point.y * blockSize + padding),
                        ((point.x + 1) * blockSize - padding),
                        ((point.y + 1) * blockSize - padding),
                        paint
                    )
                }

                // Dibujar manzanas
                val radius = (blockSize/2.5f)

                redApple?.let {
                    paint.color = Color.RED
                    canvas.drawCircle(
                        (it.position.x * blockSize + blockSize/2).toFloat(),
                        (it.position.y * blockSize + blockSize/2).toFloat(),
                        radius,
                        paint
                    )
                }

                blueApple?.let {
                    paint.color = Color.BLUE
                    canvas.drawCircle(
                        (it.position.x * blockSize + blockSize/2).toFloat(),
                        (it.position.y * blockSize + blockSize/2).toFloat(),
                        radius,
                        paint
                    )
                }

                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    private fun spawnApples() {
        // Generar posición para manzana azul
        var bluePosition: Point
        do {
            bluePosition = Point(
                Random.nextInt(0, (width/blockSize) - 1),
                Random.nextInt(0, (height/blockSize) - 1)
            )
        } while (snake.contains(bluePosition))
        blueApple = Apple(position = bluePosition, color = Color.BLUE)

        // Generar posición para manzana roja
        var redPosition: Point
        do {
            redPosition = Point(
                Random.nextInt(0, (width/blockSize) - 1),
                Random.nextInt(0, (height/blockSize) - 1)
            )
        } while (snake.contains(redPosition) || redPosition == bluePosition)
        redApple = Apple(position = redPosition, color = Color.RED)

        Log.d("SnakeGame", "Blue apple at: ${bluePosition.x}, ${bluePosition.y}")
        Log.d("SnakeGame", "Red apple at: ${redPosition.x}, ${redPosition.y}")
    }

    fun setGameCallbacks(callback: GameCallbacks) {
        this.callbacks = callback
    }

    fun pause() {
        isPlaying = false
        thread?.join()
        thread = null
    }

    fun resume() {
        if (isPlaying) return
        isPlaying = true
        thread = Thread(this)
        thread?.start()
    }

    fun resetGame() {
        pause()
        initializeGame()
        isPlaying = true
        thread = Thread(this)
        thread?.start()
    }
}