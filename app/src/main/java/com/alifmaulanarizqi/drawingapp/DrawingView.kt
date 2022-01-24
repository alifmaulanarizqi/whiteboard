package com.alifmaulanarizqi.drawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import java.util.ArrayList

class DrawingView(context: Context, attrs: AttributeSet): View(context, attrs) {
    private lateinit var mDrawPath: CustomPath
    private lateinit var mCanvasBitmap: Bitmap
    private lateinit var mDrawPaint: Paint
    private lateinit var mCanvasPaint: Paint
    private lateinit var canvas: Canvas
    private var mBrushThickness: Float = 0f
    private var mColor = Color.BLACK
    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()

    init {
        mDrawPaint = Paint()
        mDrawPaint.color = mColor
        mDrawPaint.style = Paint.Style.STROKE
        mDrawPaint.strokeJoin = Paint.Join.ROUND
        mDrawPaint.strokeCap = Paint.Cap.ROUND

        mDrawPath = CustomPath(mColor, mBrushThickness)

        mCanvasPaint = Paint(Paint.DITHER_FLAG)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(mCanvasBitmap, 0f, 0f, mCanvasPaint)

        for(path in mPaths) {
            mDrawPaint.strokeWidth = path.brushThickness
            mDrawPaint.color = path.color
            canvas?.drawPath(path, mDrawPaint)
        }

        if(!mDrawPath.isEmpty) {
            mDrawPaint.strokeWidth = mDrawPath.brushThickness
            mDrawPaint.color = mDrawPath.color
            canvas?.drawPath(mDrawPath, mDrawPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mDrawPath.color = mColor
                mDrawPath.brushThickness = mBrushThickness

                mDrawPath.reset()
                mDrawPath.moveTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_MOVE -> {
                mDrawPath.lineTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_UP -> {
                mPaths.add(mDrawPath)
                mDrawPath = CustomPath(mColor, mBrushThickness)
            }
            else -> {
                return false
            }
        }

        invalidate()

        return true
    }

    fun setBrushThickness(newbrushThickness: Float) {
        mBrushThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newbrushThickness, resources.displayMetrics)
        mDrawPaint.strokeWidth = mBrushThickness
    }

    fun setColor(newColor: String) {
        mColor = Color.parseColor(newColor)
        mDrawPaint.color = mColor
    }

    fun undo() {
        if(mPaths.size > 0) {
            mUndoPaths.add(mPaths.removeAt(mPaths.size-1))
            invalidate()
        }
    }

    fun redo() {
        if(mUndoPaths.size > 0) {
            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size-1))
            invalidate()
        }
    }

    fun clear() {
        mPaths.clear()
        mUndoPaths.clear()
        invalidate()
    }

    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path() {}

}