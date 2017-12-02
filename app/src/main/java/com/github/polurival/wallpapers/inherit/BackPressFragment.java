package com.github.polurival.wallpapers.inherit;

/**
 * Lля обработки нажатия кнопки «назад» во фрагментах.
 *
 * @author Polurival on 12.11.2017.
 */

public interface BackPressFragment {

    /**
     * @return true, если backpress был обработан или false в противном случае.
     */
    boolean handleBackPress();
}
