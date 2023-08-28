package com.example.livewallapeper.wallpaper

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Picture
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import com.example.livewallapeper.R
import kotlin.math.sqrt

class MyLiveWallpaperService : WallpaperService() {
    var startPointerCount = 0
    override fun onCreateEngine(): WallpaperService.Engine {
        return MyWallpaperEngine()
    }

    inner class MyWallpaperEngine : Engine() {
        private var wallpaperBitmap: Bitmap? = null
        private var scaleFactor = 1.0f
        private var svgPicture: Picture? = null
        private var prevFingerDistance = 0f
        private var originalScreenWidth = 0
        private var originalScreenHeight = 0
        private var longPressHandler = Handler(Looper.myLooper()!!)
        private val longPressDelay = 2


        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            try {
                val svgResource =
                    resources.openRawResource(R.raw.orange_car_ic) // Replace with your SVG resource
                val svg: SVG = SVG.getFromInputStream(svgResource)
                svgPicture = svg.renderToPicture()
            } catch (e: SVGParseException) {
                e.printStackTrace()
            }

            originalScreenWidth = surfaceHolder.surfaceFrame.width()
            originalScreenHeight = surfaceHolder.surfaceFrame.height()
            drawWallpaper()
        }

        override fun onTouchEvent(event: MotionEvent) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    startPointerCount = event.pointerCount
                    prevFingerDistance = calculateFingerDistance(event)
                    longPressHandler.postDelayed({
                        Log.e("PointerCount", "$startPointerCount")
                        if (startPointerCount == 2) {
                            startZoomIn()
                        } else if (startPointerCount == 3) {
                            startZoomOut()
                        }
                    }, longPressDelay.toLong())

                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    startPointerCount = 0
                    longPressHandler.removeCallbacksAndMessages(null)
                }

                MotionEvent.ACTION_MOVE -> {
                    startPointerCount = event.pointerCount
                    longPressHandler.postDelayed({
                        Log.e("PointerCount", "$startPointerCount")
                        if (startPointerCount == 2) {
                            startZoomIn()
                        } else if (startPointerCount == 3) {
                            startZoomOut()
                        }
                    }, longPressDelay.toLong())
                }
            }
        }

        private fun startZoomIn() {
            scaleFactor *= 1.02f
            drawWallpaper()
            longPressHandler.postDelayed({
                // startZoomIn()
                if (startPointerCount == 2) {
                    startZoomIn()
                } else if (startPointerCount == 3) {
                    startZoomOut()
                }
            }, 10)
        }

        private fun startZoomOut() {
            scaleFactor /= 1.02f
            drawWallpaper()
            longPressHandler.postDelayed({
                // startZoomOut()
                if (startPointerCount == 2) {
                    startZoomIn()
                } else if (startPointerCount == 3) {
                    startZoomOut()
                }
            }, 10)
        }

        private fun calculateFingerDistance(event: MotionEvent): Float {
            val xDiff = event.getX(0) - event.getX(1)
            val yDiff = event.getY(0) - event.getY(1)
            return sqrt(xDiff * xDiff + yDiff * yDiff)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            originalScreenWidth = width
            originalScreenHeight = height
            drawWallpaper()
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            super.onSurfaceRedrawNeeded(holder)
            drawWallpaper()
        }

        private fun drawWallpaper() {
            val surface = surfaceHolder.surface
            if (surface.isValid && svgPicture != null) {
                val canvas = surface.lockCanvas(null)
                canvas.drawColor(Color.BLACK) // Clear the canvas
                val scaledWidth = (originalScreenWidth * scaleFactor).toInt()
                val scaledHeight = (originalScreenHeight * scaleFactor).toInt()
                val left = (originalScreenWidth - scaledWidth) / 2
                val top = (originalScreenHeight - scaledHeight) / 2
                canvas.drawPicture(
                    svgPicture!!,
                    Rect(left, top, left + scaledWidth, top + scaledHeight)
                )
                surface.unlockCanvasAndPost(canvas)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                drawWallpaper()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            wallpaperBitmap?.recycle()
            wallpaperBitmap = null
        }
    }
}
