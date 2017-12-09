package com.github.polurival.wallpapers.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.booking.rtlviewpager.RtlViewPager;

public class DisableableViewPager extends RtlViewPager {

    private boolean enabled;

    public DisableableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.enabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.enabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.enabled && super.onInterceptTouchEvent(event);
    }

    public void setPagingEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
