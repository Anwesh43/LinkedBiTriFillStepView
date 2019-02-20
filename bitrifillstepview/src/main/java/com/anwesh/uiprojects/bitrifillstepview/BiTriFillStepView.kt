package com.anwesh.uiprojects.bitrifillstepview

/**
 * Created by anweshmishra on 20/02/19.
 */

import android.view.View
import android.view.MotionEvent
import android.content.Context
import android.app.Activity
import android.graphics.*

val nodes : Int = 5
val tris : Int = 2
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#4CAF50")
val backColor : Int = Color.parseColor("#212121")

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float = (1 - scaleFactor()) * a.inverse() + scaleFactor() * b.inverse()
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap
fun Int.sf() : Float = 1f - 2 * this

fun Canvas.drawTriangle(size : Float, sc :Float, paint : Paint) {
    val path : Path = Path()
    path.moveTo(0f, 0f)
    path.lineTo(size, 0f)
    path.lineTo(0f, -size)
    path.lineTo(0f, 0f)
    paint.style = Paint.Style.STROKE
    drawPath(path, paint)
    paint.style = Paint.Style.FILL
    val cp = Path()
    cp.addRect(RectF(0f, -size, size * sc, 0f), Path.Direction.CW)
    clipPath(cp)
    drawPath(path, paint)
}

fun Canvas.drawBTFNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    paint.color = foreColor
    save()
    translate(w / 2, gap * (i + 1))
    rotate(90f * sc2)
    for (j in 0..(tris - 1)) {
        save()
        scale(j.sf(), j.sf())
        drawTriangle(size, sc1.divideScale(j, 2), paint)
        restore()
    }
    restore()
}

class BiTriFillStepView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN  -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, tris, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class BTFNode(var i : Int, val state : State = State()) {

        private var next : BTFNode? = null
        private var prev : BTFNode? = null

        init {

        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = BTFNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas  : Canvas, paint : Paint) {
            canvas.drawBTFNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : BTFNode {
            var curr : BTFNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class BiTriFillStep(var i : Int) {

        private val root : BTFNode = BTFNode(0)
        private var curr : BTFNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : BiTriFillStepView) {

        private val btf : BiTriFillStep = BiTriFillStep(0)
        private val animator : Animator = Animator(view)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            btf.draw(canvas, paint)
            animator.animate {
                btf.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            btf.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : BiTriFillStepView {
            val view : BiTriFillStepView = BiTriFillStepView(activity)
            activity.setContentView(view)
            return view
        }
    }
}