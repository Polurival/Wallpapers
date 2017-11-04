package com.github.polurival.wallpapers.drawer;

import android.view.MenuItem;

import java.util.List;

/**
 * @author Polurival on 04.11.2017.
 */

public interface MenuItemCallback {

    void menuItemClicked(List<NavItem> action, MenuItem item, boolean requiresPurchase);
}
