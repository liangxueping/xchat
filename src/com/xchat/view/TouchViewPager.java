package com.xchat.view;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

public class TouchViewPager extends ViewPager {
	// mViewTouchMode表示ViewPager是否全权控制滑动事件，默认为false，即不控制
	private boolean mViewTouchMode = false;
    private int windowsWidth;
    private int padding = 20;
	private float lastMotionX;
    

	public TouchViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		// 通过Resources获取
		DisplayMetrics dm = getResources().getDisplayMetrics();
		windowsWidth = dm.widthPixels;
	}

	public void setViewTouchMode(boolean b) {
		if (b && !isFakeDragging()) {
			// 全权控制滑动事件
			beginFakeDrag();
		} else if (!b && isFakeDragging()) {
			// 终止控制滑动事件
			endFakeDrag();
		}
		mViewTouchMode = b;
	}
	/**
	 * 
	 * 在mViewTouchMode为true的时候，ViewPager不拦截点击事件，点击事件将由子View处理
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
	        case MotionEvent.ACTION_MOVE:
	        	int MAX_X = windowsWidth - padding;
	        	if ((MAX_X < lastMotionX || lastMotionX < padding) && mViewTouchMode) {
	    			return false;
	    		}
	        	break;
	        case MotionEvent.ACTION_DOWN:
	        	lastMotionX = event.getX();
	            break;
	        case MotionEvent.ACTION_CANCEL:
	            break;
	        case MotionEvent.ACTION_UP:
	        	lastMotionX = 0;
	        	break;
	        default:
	            break;
	    }
		return super.onInterceptTouchEvent(event);
	}
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		try {
			return super.onTouchEvent(ev);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 
	 * 在mViewTouchMode为true或者滑动方向不是左右的时候，ViewPager将放弃控制点击事件，
	 * 
	 * 这样做有利于在ViewPager中加入ListView等可以滑动的控件，否则两者之间的滑动将会有冲突
	 */
	@Override
	public boolean arrowScroll(int direction) {
		if (mViewTouchMode){
			return false;
		}
		if (direction != FOCUS_LEFT && direction != FOCUS_RIGHT){
			return false;
		}
		return super.arrowScroll(direction);
	}
}
