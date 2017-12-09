package com.github.polurival.wallpapers.drawer;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.github.polurival.wallpapers.MainActivity;

import java.util.List;

/**
 * Этот класс будет заполнять вкладки, отображая на них соответствующие фрагменты
 */

public class TabAdapter extends FragmentStatePagerAdapter {

    /**
     * Список пунктов панели вкладок
     */
    private List<NavItem> actions;

    private Context context;
    private Fragment mCurrentFragment;

    public TabAdapter(FragmentManager fm, List<NavItem> action, Context context) {
        super(fm);
        this.actions = action;
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        return fragmentFromAction(actions.get(position));
    }

    @Override
    public int getCount() {
        return actions.size();
    }

    /**
     * Вызывается для информирования адаптера о том,
     * какой элемент в настоящее время считается «основным»,
     * т. е. отображается в качестве текущей страницы для пользователя.
     */
    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (getCurrentFragment() != object) {
            mCurrentFragment = ((Fragment) object);
        }
        super.setPrimaryItem(container, position, object);
    }

    public Fragment getCurrentFragment() {
        return mCurrentFragment;
    }

    /**
     * @return заголовок вкладки в соответствии с позицией.
     */
    @Override
    public CharSequence getPageTitle(int position) {
        return actions.get(position).getText(context);
    }

    private Fragment fragmentFromAction(NavItem action) {
        try {
            Fragment fragment = action.getFragment().newInstance();

            Bundle args = new Bundle();
            args.putStringArray(MainActivity.FRAGMENT_DATA, action.getData());

            fragment.setArguments(args);
            return fragment;

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }
}
