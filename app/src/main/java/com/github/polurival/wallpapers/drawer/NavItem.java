package com.github.polurival.wallpapers.drawer;

import android.content.Context;
import android.support.v4.app.Fragment;

import java.io.Serializable;

/**
 * Модель пункта меню панели навигации
 *
 * @author Polurival on 04.11.2017.
 */

public class NavItem implements Serializable {

    private String mText;
    private int mTextResource;
    private String[] mData;
    private Class<? extends Fragment> mFragment;

    public String categoryImageUrl;

    public NavItem(String text, Class<? extends Fragment> fragment, String[] data) {
        mText = text;
        mFragment = fragment;
        mData = data;
    }

    public String getText(Context c) {
        if (mText != null) {
            return mText;
        } else {
            return c.getResources().getString(mTextResource);
        }
    }

    public Class<? extends Fragment> getFragment() {
        return mFragment;
    }

    public String[] getData() {
        return mData;
    }

    public void setCategoryImageUrl(String url){
        this.categoryImageUrl = url;
    }
}
