package io.github.serg987.sudokueinkhtr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun InlineDrawingCanvas(
    modifier: Modifier = Modifier,
    inkStrokes: Map<Pair<Int, Int>, List<List<DrawingPoint>>>,
    onInkStrokesChanged: (Map<Pair<Int, Int>, List<List<DrawingPoint>>>) -> Unit,
    notesMode: NotesMode,
    onDigitRecognized: (digit: Int, row: Int, col: Int) -> Unit,
    onClearCell: (row: Int, col: Int) -> Unit = { _, _ -> },
    isCellFilledWithPencil: (row: Int, col: Int) -> Boolean = { _, _ -> false },
    isCellFixed: (row: Int, col: Int) -> Boolean = { _, _ -> false }
) {
    val context = LocalContext.current
    val scaleFactor = AdaptiveSizes.getScaleFactor()

    var paths by remember { mutableStateOf(mutableListOf<List<DrawingPoint>>()) }
    var touchHelper by remember { mutableStateOf<TouchHelper?>(null) }
    var surfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    
    var viewWidth by remember { mutableIntStateOf(0) }
    var viewHeight by remember { mutableIntStateOf(0) }
    
    var lastStrokeTime by remember { mutableLongStateOf(0L) }
    var isErasing by remember { mutableStateOf(false) }
    
    // CRITICAL: We MUST use rememberUpdatedState for all lambda parameters passed to the AndroidView callbacks.
    // The AndroidView's `update` lambda only initializes the Onyx `RawInputCallback` and `onTouchListener` ONCE.
    // If we don't wrap these, those listeners will permanently capture the lambdas from the very first composition.
    // This causes bugs where starting a "New Game" creates a new `boardState`, but erasing a cell modifies the
    // discarded old `boardState` instead of the active one because it's using the stale lambda.
    val currentOnClearCell by rememberUpdatedState(onClearCell)
    val currentOnDigitRecognized by rememberUpdatedState(onDigitRecognized)
    val currentIsCellFilledWithPencil by rememberUpdatedState(isCellFilledWithPencil)
    val currentIsCellFixed by rememberUpdatedState(isCellFixed)
    val currentInkStrokes by rememberUpdatedState(inkStrokes)
    
    var eraseRow by remember { mutableIntStateOf(-1) }
    var eraseCol by remember { mutableIntStateOf(-1) }
    
    // CRITICAL: Debouncer map for onClearCell.
    // Onyx devices fire onEndRawErasing multiple times in rapid succession for a single tap. If Compose UI is
    // updated too early (while the hardware is still flashing), the E-ink screen drops the Android invalidate.
    // We debounce the state update per cell to ensure it executes exactly ONCE, strictly 600ms after the LAST hardware event.
    val clearRunnables = remember { mutableMapOf<Pair<Int, Int>, Runnable>() }

    val htrModel = remember { SettingsManager.loadHtrModel(context) }

    val recognizer = remember(htrModel) { 
        if (htrModel == HtrModel.TFLITE) DigitRecognizer(context) else null 
    }
    val onnxRecognizer = remember(htrModel) { 
        if (htrModel == HtrModel.ONNX) OnnxDigitRecognizer(context) else null 
    }
    val mlKitRecognizer = remember(htrModel) { 
        if (htrModel == HtrModel.MLKIT) MlKitDigitRecognizer(context) else null 
    }

    DisposableEffect(htrModel) {
        onDispose {
            touchHelper?.closeRawDrawing()
            recognizer?.close()
            onnxRecognizer?.close()
            mlKitRecognizer?.close()
        }
    }

    LaunchedEffect(inkStrokes, viewWidth, viewHeight) {
        if (viewWidth > 0 && viewHeight > 0) {
            surfaceView?.let { view ->
                val canvas = view.holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    val paint = Paint().apply {
                        color = Color.BLACK
                        strokeWidth = AppConfig.handwritingStrokeThickness * scaleFactor
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        isAntiAlias = true
                    }
                    for ((_, strokes) in inkStrokes) {
                        strokes.forEach { list ->
                            if (list.isNotEmpty()) {
                                val path = Path()
                                var prePoint = list[0]
                                path.moveTo(prePoint.x, prePoint.y)
                                for (i in 1 until list.size) {
                                    val point = list[i]
                                    path.quadTo(prePoint.x, prePoint.y, point.x, point.y)
                                    prePoint = point
                                }
                                canvas.drawPath(path, paint)
                            }
                        }
                    }
                    view.holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    LaunchedEffect(lastStrokeTime) {
        if (lastStrokeTime > 0) {
            Log.d("InlineDrawingCanvas", "Timeout started for lastStrokeTime=$lastStrokeTime")
            delay(500) // 500ms timeout
            Log.d("InlineDrawingCanvas", "Timeout finished. paths size=${paths.size}, viewWidth=$viewWidth, viewHeight=$viewHeight")
            if (paths.isNotEmpty() && viewWidth > 0 && viewHeight > 0) {
                // Find first point to determine row/col
                val firstPoint = paths.firstOrNull()?.firstOrNull()
                Log.d("InlineDrawingCanvas", "First point: $firstPoint")
                if (firstPoint != null) {
                    val col = (firstPoint.x / (viewWidth / 9f)).toInt().coerceIn(0, 8)
                    val row = (firstPoint.y / (viewHeight / 9f)).toInt().coerceIn(0, 8)
                    Log.d("InlineDrawingCanvas", "Mapped to row=$row, col=$col")
                    
                    if (isCellFixed(row, col)) {
                        paths = mutableListOf()
                        lastStrokeTime = 0L
                        touchHelper?.setRawDrawingEnabled(false)
                        touchHelper?.setRawDrawingEnabled(true)
                        return@LaunchedEffect
                    }
                    
                    var minX = Float.MAX_VALUE
                    var minY = Float.MAX_VALUE
                    var maxX = Float.MIN_VALUE
                    var maxY = Float.MIN_VALUE
                    paths.forEach { list ->
                        list.forEach { p ->
                            if (p.x < minX) minX = p.x
                            if (p.x > maxX) maxX = p.x
                            if (p.y < minY) minY = p.y
                            if (p.y > maxY) maxY = p.y
                        }
                    }
                    val isMicroStroke = (maxX - minX) < (20f * scaleFactor) && (maxY - minY) < (20f * scaleFactor)
                    if (isMicroStroke) {
                        Log.d("InlineDrawingCanvas", "Micro-stroke ignored")
                        paths = mutableListOf()
                        lastStrokeTime = 0L
                        touchHelper?.setRawDrawingEnabled(false)
                        touchHelper?.setRawDrawingEnabled(true)
                        return@LaunchedEffect
                    }

                    val cellWidth = viewWidth / 9f
                    val cellHeight = viewHeight / 9f
                    val toleranceX = cellWidth * 0.1f
                    val toleranceY = cellHeight * 0.1f

                    val minCol = ((minX + toleranceX) / cellWidth).toInt().coerceIn(0, 8)
                    val maxCol = ((maxX - toleranceX) / cellWidth).toInt().coerceIn(0, 8)
                    val minRow = ((minY + toleranceY) / cellHeight).toInt().coerceIn(0, 8)
                    val maxRow = ((maxY - toleranceY) / cellHeight).toInt().coerceIn(0, 8)

                    if (minCol != maxCol || minRow != maxRow) {
                        Log.d("InlineDrawingCanvas", "Stroke spans multiple cells. Ignored.")
                        paths = mutableListOf()
                        lastStrokeTime = 0L
                        touchHelper?.setRawDrawingEnabled(false)
                        touchHelper?.setRawDrawingEnabled(true)
                        return@LaunchedEffect
                    }
                    
                    val cellKey = Pair(row, col)
                    
                    if (currentIsCellFixed(row, col)) {
                        Log.d("InlineDrawingCanvas", "Cell is fixed. Ignoring handwriting.")
                        paths = mutableListOf()
                        lastStrokeTime = 0L
                        
                        if (inkStrokes.containsKey(cellKey)) {
                            val newStrokes = inkStrokes.toMutableMap()
                            newStrokes.remove(cellKey)
                            onInkStrokesChanged(newStrokes)
                        }
                        
                        touchHelper?.setRawDrawingEnabled(false)
                        touchHelper?.setRawDrawingEnabled(true)
                        return@LaunchedEffect
                    }
                    
                    if (currentIsCellFilledWithPencil(row, col) && isEraseGesture(paths, viewWidth / 9f, viewHeight / 9f)) {
                        Log.d("InlineDrawingCanvas", "Erase gesture detected for row=$row, col=$col")
                        val cellKey = Pair(row, col)
                        val newStrokes = currentInkStrokes.toMutableMap()
                        newStrokes.remove(cellKey)
                        onInkStrokesChanged(newStrokes)
                        
                        clearRunnables[cellKey]?.let { surfaceView?.removeCallbacks(it) }
                        val r = Runnable {
                            Log.d("InlineDrawingCanvas", "currentOnClearCell executing safely after debounce (gesture)")
                            currentOnClearCell(row, col)
                        }
                        clearRunnables[cellKey] = r
                        surfaceView?.postDelayed(r, 600)
                        
                        touchHelper?.setRawDrawingEnabled(false)
                        touchHelper?.setRawDrawingEnabled(true)
                        
                        paths = mutableListOf()
                        lastStrokeTime = 0L
                        return@LaunchedEffect
                    }
                    
                    val existingStrokes = currentInkStrokes[cellKey]?.toMutableList() ?: mutableListOf()
                    existingStrokes.addAll(paths)
                    
                    if (notesMode == NotesMode.MANUAL) {
                        Log.d("InlineDrawingCanvas", "Manual notes mode. Bypassing recognition.")
                        onInkStrokesChanged(currentInkStrokes + (cellKey to existingStrokes))
                    } else {
                        val strokeWidth = 20f * scaleFactor
                        val digit = withContext(Dispatchers.IO) {
                            val bitmap = inlinePathsToBitmap(paths, viewWidth, viewHeight, strokeWidth)
                            
                            when (htrModel) {
                                HtrModel.TFLITE -> {
                                    recognizer?.recognizeDigit(bitmap) ?: -1
                                }
                                HtrModel.ONNX -> {
                                    val onnxResult = onnxRecognizer?.recognizeDigit(bitmap)
                                    val digitResult = onnxResult?.digit ?: -1
                                    // Treat blank class (10) as unrecognized or zero
                                    if (digitResult == 10) -1 else digitResult
                                }
                                HtrModel.MLKIT -> {
                                    val mlKitResult = mlKitRecognizer?.recognizeDigitAsync(paths, viewWidth / 9f, viewHeight / 9f)
                                    mlKitResult?.digit ?: -1
                                }
                            }
                        }
                        Log.d("InlineDrawingCanvas", "Recognized digit (used for app): $digit using model $htrModel")
                        
                        onDigitRecognized(digit, row, col)
                        onInkStrokesChanged(currentInkStrokes + (cellKey to existingStrokes))
                    }
                }
                
                // Clear state
                paths = mutableListOf()
                lastStrokeTime = 0L
                
                touchHelper?.setRawDrawingEnabled(false)
                touchHelper?.setRawDrawingEnabled(true)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SurfaceView(ctx).apply {
                setZOrderOnTop(true)
                holder.setFormat(PixelFormat.TRANSPARENT)
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        val canvas = holder.lockCanvas()
                        canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                        if (canvas != null) {
                            holder.unlockCanvasAndPost(canvas)
                        }
                    }
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                    override fun surfaceDestroyed(holder: SurfaceHolder) {}
                })
                
                setOnTouchListener { _, event ->
                    val isEraser = event.getToolType(0) == android.view.MotionEvent.TOOL_TYPE_ERASER ||
                        (event.getToolType(0) == android.view.MotionEvent.TOOL_TYPE_STYLUS && 
                        (event.buttonState and android.view.MotionEvent.BUTTON_STYLUS_PRIMARY != 0 || 
                         event.buttonState and android.view.MotionEvent.BUTTON_SECONDARY != 0))
                    
                    Log.d("InlineDrawingCanvas", "onTouch: actionMasked=${event.actionMasked}, isEraser=$isEraser, buttonState=${event.buttonState}, toolType=${event.getToolType(0)}")
                         
                    if (isEraser || isErasing) {
                        if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN && isEraser) {
                            Log.d("InlineDrawingCanvas", "onTouch ACTION_DOWN (eraser). Setting isErasing=true, clearing cell")
                            isErasing = true
                            paths = mutableListOf()
                            lastStrokeTime = 0L
                            
                            val col = (event.x / (viewWidth / 9f)).toInt().coerceIn(0, 8)
                            val row = (event.y / (viewHeight / 9f)).toInt().coerceIn(0, 8)
                            Log.d("InlineDrawingCanvas", "onTouch ACTION_DOWN (eraser). Setting isErasing=true, clearing cell row=$row, col=$col")
                            
                            eraseRow = row
                            eraseCol = col
                            
                            val cellKey = Pair(row, col)
                            val newStrokes = currentInkStrokes.toMutableMap()
                            newStrokes.remove(cellKey)
                            onInkStrokesChanged(newStrokes)
                            
                            // We delay the onClearCell to ACTION_UP to avoid e-ink refresh conflicts
                            
                            touchHelper?.setRawDrawingEnabled(false)
                            touchHelper?.setRawDrawingEnabled(true)
                            
                            return@setOnTouchListener true
                        } else if (event.actionMasked == android.view.MotionEvent.ACTION_UP || event.actionMasked == android.view.MotionEvent.ACTION_CANCEL) {
                            Log.d("InlineDrawingCanvas", "onTouch ACTION_UP/CANCEL (eraser). Setting isErasing=false after 500ms delay")
                            paths = mutableListOf()
                            lastStrokeTime = 0L
                            
                            val row = if (eraseRow != -1) eraseRow else (event.y / (viewHeight / 9f)).toInt().coerceIn(0, 8)
                            val col = if (eraseCol != -1) eraseCol else (event.x / (viewWidth / 9f)).toInt().coerceIn(0, 8)
                            
                            val cellKey = Pair(row, col)
                            clearRunnables[cellKey]?.let { removeCallbacks(it) }
                            val r = Runnable {
                                Log.d("InlineDrawingCanvas", "currentOnClearCell executing safely after debounce (ACTION_UP)")
                                // Invoking the current lambda using rememberUpdatedState to avoid stale boardState
                                currentOnClearCell(row, col)
                            }
                            clearRunnables[cellKey] = r
                            // 600ms delay ensures native e-ink hardware erase completely finishes before Android renders
                            postDelayed(r, 600)
                            postDelayed({ isErasing = false }, 600)
                            
                            eraseRow = -1
                            eraseCol = -1
                            return@setOnTouchListener true
                        }
                    }
                    false
                }
                
                surfaceView = this
            }
        },
        update = { view ->
            if (touchHelper == null) {
                view.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                        v: View, left: Int, top: Int, right: Int, bottom: Int,
                        oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                    ) {
                        view.removeOnLayoutChangeListener(this)
                        viewWidth = right - left
                        viewHeight = bottom - top
                        
                        val limit = Rect()
                        view.getLocalVisibleRect(limit)
                        
                        val callback = object : RawInputCallback() {
                            override fun onBeginRawDrawing(b: Boolean, touchPoint: TouchPoint) {
                                Log.d("InlineDrawingCanvas", "onBeginRawDrawing. isErasing=$isErasing")
                                if (isErasing) return
                            }
                            override fun onEndRawDrawing(b: Boolean, touchPoint: TouchPoint) {
                                Log.d("InlineDrawingCanvas", "onEndRawDrawing. isErasing=$isErasing")
                                if (isErasing) return
                                lastStrokeTime = System.currentTimeMillis()
                            }
                            override fun onRawDrawingTouchPointMoveReceived(touchPoint: TouchPoint) {}
                            override fun onRawDrawingTouchPointListReceived(touchPointList: TouchPointList) {
                                Log.d("InlineDrawingCanvas", "onRawDrawingTouchPointListReceived size=${touchPointList.points.size}, isErasing=$isErasing")
                                if (isErasing) return
                                val points = touchPointList.points.map { DrawingPoint(it.x, it.y, it.timestamp) }
                                val currentPaths = paths.toMutableList()
                                currentPaths.add(points)
                                paths = currentPaths
                            }
                            override fun onBeginRawErasing(b: Boolean, touchPoint: TouchPoint) {
                                Log.d("InlineDrawingCanvas", "onBeginRawErasing")
                                isErasing = true
                                paths = mutableListOf()
                                lastStrokeTime = 0L
                            }
                            override fun onEndRawErasing(b: Boolean, touchPoint: TouchPoint) {
                                Log.d("InlineDrawingCanvas", "onEndRawErasing")
                                val col = (touchPoint.x / (viewWidth / 9f)).toInt().coerceIn(0, 8)
                                val row = (touchPoint.y / (viewHeight / 9f)).toInt().coerceIn(0, 8)
                                Log.d("InlineDrawingCanvas", "onEndRawErasing. row=$row, col=$col")
                                
                                val cellKey = Pair(row, col)
                                val newStrokes = currentInkStrokes.toMutableMap()
                                newStrokes.remove(cellKey)
                                onInkStrokesChanged(newStrokes)
                                
                                clearRunnables[cellKey]?.let { surfaceView?.removeCallbacks(it) }
                                val r = Runnable {
                                    Log.d("InlineDrawingCanvas", "currentOnClearCell executing safely after debounce (onEndRawErasing)")
                                    // Invoking the current lambda using rememberUpdatedState to avoid stale boardState
                                    currentOnClearCell(row, col)
                                }
                                clearRunnables[cellKey] = r
                                // 600ms delay ensures native e-ink hardware erase completely finishes before Android renders
                                surfaceView?.postDelayed(r, 600)
                                
                                touchHelper?.setRawDrawingEnabled(false)
                                touchHelper?.setRawDrawingEnabled(true)
                                
                                paths = mutableListOf()
                                lastStrokeTime = 0L
                                surfaceView?.postDelayed({ isErasing = false }, 500)
                            }
                            override fun onRawErasingTouchPointMoveReceived(touchPoint: TouchPoint) {}
                            override fun onRawErasingTouchPointListReceived(touchPointList: TouchPointList) {}
                        }
                        
                        val th = TouchHelper.create(view, callback)
                        val strokeWidth = 20f * scaleFactor
                        th.setStrokeWidth(strokeWidth)
                          .setLimitRect(limit, ArrayList<Rect>())
                          .openRawDrawing()
                        th.setStrokeStyle(TouchHelper.STROKE_STYLE_FOUNTAIN)
                        th.setRawDrawingEnabled(true)
                        th.setRawDrawingRenderEnabled(true)
                        
                        touchHelper = th
                    }
                })
            }
        }
    )
}

private fun inlinePathsToBitmap(pathsList: List<List<DrawingPoint>>, width: Int, height: Int, strokeWidth: Float): Bitmap {
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    pathsList.forEach { list ->
        list.forEach { p ->
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }
    }
    
    // Fallback if no points
    if (minX > maxX || minY > maxY) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)
        return bitmap
    }
    
    val padding = strokeWidth * 2
    minX = (minX - padding).coerceAtLeast(0f)
    minY = (minY - padding).coerceAtLeast(0f)
    maxX = (maxX + padding).coerceAtMost(width.toFloat())
    maxY = (maxY + padding).coerceAtMost(height.toFloat())
    
    val cropWidth = (maxX - minX).toInt().coerceAtLeast(1)
    val cropHeight = (maxY - minY).toInt().coerceAtLeast(1)
    
    val bitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.BLACK)

    val paint = Paint().apply {
        color = Color.WHITE
        this.strokeWidth = strokeWidth
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    pathsList.forEach { list ->
        if (list.isNotEmpty()) {
            val path = Path()
            var prePoint = list[0]
            path.moveTo(prePoint.x - minX, prePoint.y - minY)
            for (i in 1 until list.size) {
                val point = list[i]
                path.quadTo(prePoint.x - minX, prePoint.y - minY, point.x - minX, point.y - minY)
                prePoint = point
            }
            canvas.drawPath(path, paint)
        }
    }

    return bitmap
}

private fun isEraseGesture(pathsList: List<List<DrawingPoint>>, cellWidth: Float, cellHeight: Float): Boolean {
    if (pathsList.isEmpty()) return false
    
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    
    pathsList.forEach { list ->
        list.forEach { p ->
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }
    }
    
    // 1. Strike-through (-)
    if (pathsList.size == 1) {
        val stroke = pathsList[0]
        if (stroke.size > 2) {
            val startP = stroke.first()
            val endP = stroke.last()
            val dx = kotlin.math.abs(endP.x - startP.x)
            val dy = kotlin.math.abs(endP.y - startP.y)
            if (dx > cellWidth * 0.4f) {
                // Check if it's within 60 degrees of horizontal (tan 60 = 1.732)
                if (dy <= dx * 1.75f) {
                    return true
                }
            }
        }
    }
    
    // 2. Scribble (zig-zag)
    // Works across multiple strokes
    var reversals = 0
    var lastDir = 0
    pathsList.forEach { stroke ->
        for (i in 1 until stroke.size) {
            val dx = stroke[i].x - stroke[i-1].x
            if (kotlin.math.abs(dx) > 5f) {
                val currentDir = if (dx > 0) 1 else -1
                if (lastDir != 0 && currentDir != lastDir) {
                    reversals++
                }
                lastDir = currentDir
            }
        }
    }
    if (reversals >= 4) {
        val dxTotal = maxX - minX
        if (dxTotal > cellWidth * 0.3f) {
            return true
        }
    }
    
    // 3. Cross out (X)
    if (pathsList.size == 2) {
        val stroke1 = pathsList[0]
        val stroke2 = pathsList[1]
        
        fun isDiagonal(s: List<DrawingPoint>): Boolean {
            if (s.size < 2) return false
            val dx = kotlin.math.abs(s.last().x - s.first().x)
            val dy = kotlin.math.abs(s.last().y - s.first().y)
            return dx > cellWidth * 0.3f && dy > cellHeight * 0.3f
        }
        
        if (isDiagonal(stroke1) && isDiagonal(stroke2)) {
            val s1MinX = stroke1.minOf { it.x }
            val s1MaxX = stroke1.maxOf { it.x }
            val s1MinY = stroke1.minOf { it.y }
            val s1MaxY = stroke1.maxOf { it.y }
            
            val s2MinX = stroke2.minOf { it.x }
            val s2MaxX = stroke2.maxOf { it.x }
            val s2MinY = stroke2.minOf { it.y }
            val s2MaxY = stroke2.maxOf { it.y }
            
            if (s1MinX < s2MaxX && s1MaxX > s2MinX && s1MinY < s2MaxY && s1MaxY > s2MinY) {
                return true
            }
        }
    }
    
    return false
}
