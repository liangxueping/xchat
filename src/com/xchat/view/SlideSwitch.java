package com.xchat.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.xchat.activity.R;

public class SlideSwitch extends FrameLayout implements View.OnTouchListener {
	private boolean mChecked = false;
	private float nowX, downX;
	private View ballView;
	private View onBgView;
	private int ballGetLeft;
	private OnCheckedChangeListener mCheckListener;
	private int specWidth;
	private int specHeight;
	private boolean wrapLp;
	private long downTime;
	private static final long CLICK_TIME_GAP = 150;
	private boolean layouted;

	public SlideSwitch(Context context, AttributeSet attrs) {
		super(context, attrs);
		try {
			int lpWidth = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "layout_width", 0);
			int lpHeight = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "layout_height", 0);
			mChecked = attrs.getAttributeBooleanValue("http://schemas.android.com/apk/res/android", "checked", false);
			if (lpWidth == LayoutParams.WRAP_CONTENT ||
					lpHeight == LayoutParams.WRAP_CONTENT) {
				wrapLp = true;
				Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.slide_switch_on_bg);
				specWidth = bmp.getWidth();
				specHeight = bmp.getHeight();
				bmp.recycle();
				bmp = null;
			}
		} catch (Exception e) {}
		init();
	}

	private void init() {
		setBackgroundResource(R.drawable.slide_switch_off_bg);
		
		onBgView = new ImageView(getContext());
		onBgView.setBackgroundResource(R.drawable.slide_switch_on_bg);
		addView(onBgView);
		
		ballView = new ImageView(getContext());
		ballView.setBackgroundResource(R.drawable.slide_switch_ball);
		addView(ballView);
		
		setOnTouchListener(this);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (wrapLp) {
			widthMeasureSpec = specWidth;
			heightMeasureSpec = specHeight;
		}
		View bottomChild = getChildAt(0);
		bottomChild.measure(widthMeasureSpec, heightMeasureSpec);
		int w = bottomChild.getMeasuredWidth();
		int h = bottomChild.getMeasuredHeight();
		View topChild = getChildAt(1);
		topChild.measure(heightMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		layouted = true;
		if (mChecked) {
			ballView.layout(getWidth() - ballView.getWidth(), 0, getWidth(), ballView.getHeight());
			onBgView.layout(0, 0, onBgView.getWidth(), onBgView.getHeight());
		} else {
			onBgView.layout(-onBgView.getWidth(), 0, 0, onBgView.getHeight());
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_MOVE:
//				if (event.getX() > v.getWidth() || event.getY() > v.getHeight()
//						|| event.getX() < 0 || event.getY() < 0) {
//					return false;
//				}
				nowX = event.getX();
				if (ballGetLeft + nowX - downX < 0) {
					ballView.layout(0, 0, ballView.getWidth(), ballView.getHeight());
					return false;
				} else if (ballGetLeft + nowX - downX > v.getWidth() - ballView.getWidth()) {
					ballView.layout(v.getWidth() - ballView.getWidth(), 0, v.getWidth(), ballView.getHeight());
					return false;
				}
				ballView.layout((int) (ballGetLeft + nowX - downX), 0, (int) (ballGetLeft + ballView.getWidth() + nowX - downX),
						ballView.getHeight());
				if (ballView.getLeft() >= (v.getWidth() - ballView.getWidth()) / 2) {
					onBgView.layout(0, 0, onBgView.getWidth(), onBgView.getHeight());
				} else {
					onBgView.layout(-onBgView.getWidth(), 0, 0, onBgView.getHeight());
				}
				break;
				
			case MotionEvent.ACTION_DOWN:
				downX = event.getX();
				ballGetLeft = ballView.getLeft();
				downTime = System.currentTimeMillis();
				break;
				
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				if (ballView.getLeft() >= (v.getWidth() - ballView.getWidth()) / 2) {
					ballView.layout(v.getWidth() - ballView.getWidth(), 0, v.getWidth(), ballView.getHeight());
					onBgView.layout(0, 0, onBgView.getWidth(), onBgView.getHeight());
					if (!mChecked) {
						mChecked = true;
						if (mCheckListener != null) {
							mCheckListener.onCheckedChanged(this, true);
						}
						return true;
					}
				} else {
					ballView.layout(0, 0, ballView.getWidth(), ballView.getHeight());
					onBgView.layout(-onBgView.getWidth(), 0, 0, onBgView.getHeight());
					if (mChecked) {
						mChecked = false;
						if (mCheckListener != null) {
							mCheckListener.onCheckedChanged(this, false);
						}
						return true;
					}
				}
				
				if (System.currentTimeMillis() - downTime <= CLICK_TIME_GAP) {
					setCheckedByClick();
				}
				
				break;
		}
		return true;
	}

	private void setCheckedByClick() {
		mChecked = !mChecked;
		if (mChecked) {
			ballView.layout(getWidth() - ballView.getWidth(), 0, getWidth(), ballView.getHeight());
			onBgView.layout(0, 0, onBgView.getWidth(), onBgView.getHeight());
		} else {
			ballView.layout(0, 0, ballView.getWidth(), ballView.getHeight());
			onBgView.layout(-onBgView.getWidth(), 0, 0, onBgView.getHeight());
		}
		if (mCheckListener != null) {
			mCheckListener.onCheckedChanged(this, mChecked);
		}
	}
	
	public void setChecked(boolean checked) {
		mChecked = checked;
		if (layouted) {
			requestLayout();
		}
	}
	
	public boolean isChecked() {
		return mChecked;
	}

	public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
		mCheckListener = listener;
	}

	public interface OnCheckedChangeListener {
		void onCheckedChanged(View v, boolean checked);
	}
}