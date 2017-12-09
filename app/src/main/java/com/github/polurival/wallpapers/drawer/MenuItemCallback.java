package com.github.polurival.wallpapers.drawer;

import android.view.MenuItem;

import java.util.List;

public interface MenuItemCallback {

    void menuItemClicked(List<NavItem> action, MenuItem item, boolean requiresPurchase);
}
