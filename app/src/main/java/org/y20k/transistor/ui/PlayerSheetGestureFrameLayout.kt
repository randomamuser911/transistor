/*
 * PlayerSheetGestureFrameLayout.kt
 * Implements the PlayerSheetGestureFrameLayout class
 * A FrameLayout that adds swipe gestures to the now playing sheet:
 * dragging or flinging down closes the sheet, horizontal flings switch stations.
 * Vertical drags are handled through the nested scrolling protocol: the inner
 * scroll view scrolls its content until it reaches the top, then hands the
 * unconsumed movement to this view, which moves the sheet with the finger.
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-26 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.ui

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.NestedScrollingParent3
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.TYPE_TOUCH


/*
 * PlayerSheetGestureFrameLayout class
 */
class PlayerSheetGestureFrameLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): FrameLayout(context, attrs, defStyleAttr), NestedScrollingParent3 {

    /* Listener Interface */
    interface GestureListener {
        fun onSheetDrag(sheetOffsetY: Float)
        fun onSheetDragEnded(shouldClose: Boolean)
        fun onSwipeToNextStation()
        fun onSwipeToPreviousStation()
    }

    /* Main class variables */
    var gestureListener: GestureListener? = null
    private var sheetOffsetY: Float = 0f
    private var settleAnimator: ValueAnimator? = null
    private var horizontalSwipeDetected: Boolean = false

    /* Velocity threshold below which a release counts as a drag, not a fling */
    private val closeFlingVelocity: Float = 1000f


    /* Detects horizontal flings that switch stations - fed from dispatchTouchEvent so it sees the full gesture even while the scroll view consumes the drags */
    private val flingDetector: GestureDetector by lazy {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (horizontalSwipeDetected && sheetOffsetY == 0f && kotlin.math.abs(velocityX) > kotlin.math.abs(velocityY) && kotlin.math.abs(velocityX) > 2500f) {
                    if (velocityX < 0f) gestureListener?.onSwipeToNextStation() else gestureListener?.onSwipeToPreviousStation()
                    return true
                }
                return false
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                // remember gestures that clearly move sideways - they are meant to switch stations
                horizontalSwipeDetected = kotlin.math.abs(e2.x - (e1?.x ?: e2.x)) > kotlin.math.abs(e2.y - (e1?.y ?: e2.y)) + 48f
                return false
            }
        })
    }


    /* Overrides dispatchTouchEvent - feeds the fling detector with the complete touch stream */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                horizontalSwipeDetected = false
                settleAnimator?.cancel()
            }
        }
        flingDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }


    /* Overrides onStartNestedScroll from NestedScrollingParent3 - accepts vertical touch scrolls of the inner scroll view */
    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0 && type == TYPE_TOUCH
    }


    /* Overrides onNestedScrollAccepted from NestedScrollingParent3 */
    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        settleAnimator?.cancel()
    }


    /* Overrides onStopNestedScroll from NestedScrollingParent3 - settles the sheet after the finger lifts */
    override fun onStopNestedScroll(target: View, type: Int) {
        if (sheetOffsetY > 0f && settleAnimator == null) {
            settleSheet()
        }
    }


    /* Overrides onNestedScroll from NestedScrollingParent3 (part of the interface) */
    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray) {
        // scrolling already handled in onNestedPreScroll
    }


    /* Overrides onNestedScroll from NestedScrollingParent2 */
    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
        // scrolling already handled in onNestedPreScroll
    }


    /* Overrides onNestedPreScroll from NestedScrollingParent3 - moves the sheet once the content is scrolled to the top */
    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (dy < 0 && !target.canScrollVertically(-1)) {
            // finger pulls down, content is at the top - the sheet follows the finger
            consumed[1] = dy
            sheetOffsetY = (sheetOffsetY - dy).coerceAtLeast(0f)
            gestureListener?.onSheetDrag(sheetOffsetY)
        } else if (sheetOffsetY > 0f && dy > 0) {
            // sheet is moved and the finger travels back up - return it to its resting place
            val giveBack: Int = dy.coerceAtMost(sheetOffsetY.toInt())
            consumed[1] = giveBack
            sheetOffsetY -= giveBack
            gestureListener?.onSheetDrag(sheetOffsetY)
        }
        // finger moves up while the sheet rests - let the content scroll
    }


    /* Overrides onNestedPreFling from NestedScrollingParent3 - closes the sheet on downward flings */
    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        if (sheetOffsetY > 0f) {
            settleSheet(velocityY)
            return true
        }
        return false
    }


    /* Settles the sheet - closes it when far enough down or fast enough, springs back otherwise */
    private fun settleSheet(releaseVelocityY: Float = 0f) {
        val shouldClose: Boolean = sheetOffsetY > height / 6f || releaseVelocityY > closeFlingVelocity
        if (shouldClose) {
            gestureListener?.onSheetDragEnded(shouldClose = true)
            sheetOffsetY = 0f
        } else {
            animateSheetTo(0f)
        }
    }


    /* Animates the sheet back to the given offset */
    private fun animateSheetTo(targetOffsetY: Float) {
        settleAnimator?.cancel()
        val animator: ValueAnimator = ValueAnimator.ofFloat(sheetOffsetY, targetOffsetY)
        animator.duration = 250L
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            sheetOffsetY = animation.animatedValue as Float
            gestureListener?.onSheetDrag(sheetOffsetY)
        }
        animator.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) { }
            override fun onAnimationEnd(animation: android.animation.Animator) {
                settleAnimator = null
                if (sheetOffsetY < 1f) {
                    sheetOffsetY = 0f
                    gestureListener?.onSheetDrag(0f)
                }
            }
            override fun onAnimationCancel(animation: android.animation.Animator) { }
            override fun onAnimationRepeat(animation: android.animation.Animator) { }
        })
        settleAnimator = animator
        animator.start()
    }

}
