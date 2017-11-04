package com.github.polurival.wallpapers;

import com.github.polurival.wallpapers.drawer.SimpleMenu;

/**
 * @author Polurival on 04.11.2017.
 */

public class Config {

    public static String CONFIG_URL = "";


    public static final boolean HIDE_DRAWER = false;


    public static boolean USE_HARDCODED_CONFIG = false;


    public static void configureMenu(SimpleMenu menu, ConfigParser.CallBack callback){


        callback.configLoaded(false);
    }
}
