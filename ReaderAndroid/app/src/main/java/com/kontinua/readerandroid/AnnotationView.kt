package com.kontinua.readerandroid

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlin.math.sqrt

class AnnotationView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    // data to serialize scribbles
    private data class Stroke(
        val points: List<Pair<Float, Float>>,
        val color: Int,
        val alpha: Int,
        val strokeWidth: Float,
    ) {
        fun toPath(): Path {
            val path = Path()
            if (points.isNotEmpty()) {
                path.moveTo(points[0].first, points[0].second)
                for (i in 1 until points.size) {
                    path.lineTo(points[i].first, points[i].second)
                }
            }
            return path
        }

        fun toPaint(): Paint = Paint().apply {
            color = this@Stroke.color
            alpha = this@Stroke.alpha
            strokeWidth = this@Stroke.strokeWidth
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
    }

    private val annotations = HashMap<String, MutableList<Stroke>>() // Stores strokes per page for workbook
    private var currentPage = 0
    private var currentWorkbook = "workbook-01.pdf"
    private var currentKey = "workbook-01.pdf-0"
    private var currentPath = Path()
    private var currentPaint = createPaint(Color.TRANSPARENT, 0, 30f)

    private fun createPaint(color: Int, alpha: Int, strokeWidth: Float): Paint = Paint().apply {
        this.color = color
        this.alpha = alpha
        this.strokeWidth = strokeWidth
        this.style = Paint.Style.STROKE
        this.isAntiAlias = true
    }

    private var drawingEnabled = false // Default: Disabled
    private var eraseMode = false

    // Method to enable/disable scribbles
    fun setDrawingMode(enabled: Boolean, type: String, color: Int) {
        drawingEnabled = enabled
        eraseMode = false
        if (type == "pen") {
            currentPaint = createPaint(color, 255, 10f)
        } else if (type == "highlight") {
            currentPaint = createPaint(color, 128, 20f)
        }
    }

    fun setEraseMode(enabled: Boolean) {
        drawingEnabled = enabled
        eraseMode = true
    }

    private fun removeStrokeNearPoint(x: Float, y: Float) {
        val strokeList = annotations[currentKey] ?: return
        val iterator = strokeList.iterator()

        while (iterator.hasNext()) {
            val stroke = iterator.next()

            // Check if any point in the stroke is near the user's touch
            for (point in stroke.points) {
                val dx = point.first - x
                val dy = point.second - y
                val distance = sqrt((dx * dx + dy * dy).toDouble())

                if (distance < 30) {
                    iterator.remove()
                    invalidate()
                    saveAnnotations()
                    return
                }
            }
        }
    }

    // clear screen function
    fun clearCanvas() {
        annotations[currentKey]?.clear()
        invalidate()
    }

    fun setPage(page: Int) {
        saveAnnotations()
        currentPage = page
        currentKey = "$currentWorkbook-$currentPage"
        loadAnnotations()
        invalidate()
    }

    fun setWorkbook(workbookName: String) {
        saveAnnotations()
        currentWorkbook = workbookName
        setPage(0)
        invalidate()
    }

    fun saveAnnotations() {
        try {
            val gson = Gson()
            val file = File(context.filesDir, "annotations.json")

            Log.d("AnnotationView", "Saving annotations for workbook and page: $currentKey")

            val json = gson.toJson(annotations)
            file.writeText(json)
        } catch (e: Exception) {
            Log.e("AnnotationView", "Error saving annotations: ${e.message}")
        }
    }

    fun loadAnnotations() {
        val file = File(context.filesDir, "annotations.json")
        if (!file.exists()) {
            Log.d("AnnotationView", "No saved annotations for workbook and page: $currentKey")
            return
        }

        try {
            val gson = Gson()
            val type = object : TypeToken<HashMap<String, MutableList<Stroke>>>() {}.type

            val loadedAnnotations: HashMap<String, MutableList<Stroke>> = gson.fromJson(file.readText(), type)

            annotations.clear()
            annotations.putAll(loadedAnnotations)
            Log.d("AnnotationView", "Loaded annotations for workbook and page: $currentKey")
        } catch (e: Exception) {
            Log.e("AnnotationView", "Error loading annotations: ${e.message}")
        }

        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!drawingEnabled) return false

        if (eraseMode) {
            if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
                removeStrokeNearPoint(event.x, event.y)
            }
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path().apply { moveTo(event.x, event.y) }
                val stroke = Stroke(
                    points = mutableListOf(Pair(event.x, event.y)),
                    color = currentPaint.color,
                    alpha = currentPaint.alpha,
                    strokeWidth = currentPaint.strokeWidth,
                )
                annotations.getOrPut(currentKey) { mutableListOf() }.add(stroke)
            }
            MotionEvent.ACTION_MOVE -> {
                val strokeList = annotations[currentKey]
                if (!strokeList.isNullOrEmpty()) {
                    val lastStroke = strokeList.last()
                    val updatedPoints = lastStroke.points.toMutableList()
                    updatedPoints.add(Pair(event.x, event.y))

                    // Replace with updated stroke
                    strokeList[strokeList.size - 1] = lastStroke.copy(points = updatedPoints)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                saveAnnotations()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val saveLayer = canvas.saveLayer(null, null)

        annotations[currentKey]?.forEach { stroke ->
            val path = stroke.toPath()
            val paint = stroke.toPaint()
            canvas.drawPath(path, paint)
        }

        canvas.restoreToCount(saveLayer)
    }
}
