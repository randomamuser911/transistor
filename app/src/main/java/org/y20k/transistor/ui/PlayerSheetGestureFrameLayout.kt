/*
 * PlayerSheetGestureFrameLayout.kt
 * Implements the PlayerSheetGestureFrameLayout class
 * A FrameLayout that adds swipe gestures to the now playing sheet:
 * drag or fling down closes the sheet, horizontal flings switch stations.
 * Touches that start on interactive controls (sliders, buttons) are never intercepted.
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-26 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.ui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ScrollView
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.slider.Slider
import org.y20k.transistor.R


/*
 * PlayerSheetGestureFrameLayout class
 */
class PlayerSheetGestureFrameLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): FrameLayout(context, attrs, defStyleAttr) {

    /* Listener Interface */
    interface GestureListener {
        fun onSheetDrag(dy: Float)
        fun onSheetDragEnded(shouldClose: Boolean)
        fun onSwipeToNextStation()
        fun onSwipeToPreviousStation()
    }

    /* Main class variables */
    var gestureListener: GestureListener? = null
    private var touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private var downX: Float = 0f
    private var downY: Float = 0f
    private var draggingSheet: Boolean = false
    private var gestureIgnored: Boolean = false
    private var horizontalSwipe: Boolean = false
    private var verticalDrag: Boolean = false

    /* Detects flings */
    private val flingDetector: GestureDetector by lazy {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (gestureIgnored || draggingSheet || verticalDrag) return false
                val listener: GestureListener = gestureListener ?: return false
                // horizontal fling switches stations
                if (kotlin.math.abs(velocityX) > 2500f && kotlin.math.abs(velocityX) > kotlin.math.abs(velocityY)) {
                    if (velocityX < 0f) listener.onSwipeToNextStation() else listener.onSwipeToPreviousStation()
                    return true
                }
                // downward fling closes the sheet
                if (velocityY > 2500f && kotlin.math.abs(velocityY) > kotlin.math.abs(velocityX)) {
                    listener.onSheetDragEnded(shouldClose = true)
                    return true
                }
                return false
            }
        })
    }


    /* Overrides onInterceptTouchEvent - steals drags that are meant for the sheet, passes on everything else */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        flingDetector.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                draggingSheet = false
                horizontalSwipe = false
                verticalDrag = false
                gestureIgnored = isTouchOnInteractiveView(ev.x, ev.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (gestureIgnored || draggingSheet) return draggingSheet
                val dx: Float = ev.x - downX
                val dy: Float = ev.y - downY
                // decide whether this gesture belongs to the sheet once the finger has moved past the touch slop
                if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) {
                    if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                        // horizontal swipe: let the fling detector handle it later - do not intercept
                        horizontalSwipe = true
                    } else if (dy > 0 && isContentAtTop()) {
                        // downward drag: sheet follows the finger
                        verticalDrag = true
                        draggingSheet = true
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    } else {
                        // upward drag: let the scroll view handle it
                        gestureIgnored = true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (draggingSheet) {
                    draggingSheet = false
                    gestureListener?.onSheetDragEnded(shouldClose = false)
                }
            }
        }
        return false
    }


    /* Overrides onTouchEvent - receives the events of a stolen drag */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!draggingSheet) return flingDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                gestureListener?.onSheetDrag(event.y - downY)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggingSheet = false
                val totalDrag: Float = event.y - downY
                gestureListener?.onSheetDragEnded(shouldClose = totalDrag > height / 4)
                performClick()
                return true
            }
        }
        return true
    }


    /* Overrides performClick for accessibility */
    override fun performClick(): Boolean {
        return super.performClick()
    }


    /* Checks whether the given point lies on a view that should receive the touch itself */
    private fun isTouchOnInteractiveView(x: Float, y: Float): Boolean {
        var interactive: Boolean = false
        traverseChildren(this) { child ->
            if (child.isClickable || child is Slider || child is android.widget.Button || (child is android.widget.TextView && ViewCompat.hasOnClickListeners(child))) {
                val hitRect: Rect = Rect()
                child.getHitRect(hitRect)
                if (hitRect.contains(x.toInt(), y.toInt())) {
                    interactive = true
                    return@traverseChildren
                }
            }
        }
        return interactive
    }


    /* Walks a view tree and calls visitor on every descendant */
    private fun traverseChildren(view: View, visitor: (View) -> Unit) {
        if (view is ViewGroup) {
            for (index: Int in 0 until view.childCount) {
                val child: View = view.getChildAt(index)
                visitor(child)
                traverseChildren(child, visitor)
            }
        }
    }


    /* Checks whether the sheet's scrollable content is scrolled to the top */
    private fun isContentAtTop(): Boolean {
        val scrollView: View? = findViewById(R.id.player_sheet_scroll)
        return when (scrollView) {
            is NestedScrollView -> scrollView.scrollY == 0
            is ScrollView -> scrollView.scrollY == 0
            else -> true
        }
    }

}
