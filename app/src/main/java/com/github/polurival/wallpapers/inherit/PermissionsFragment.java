package com.github.polurival.wallpapers.inherit;

/**
 * Для запроса разрешений для фрагментов
 *
 * @author Polurival on 12.11.2017.
 */

public interface PermissionsFragment {

    /**
     * @return массив всех разрешений, необходимых для этого фрагмента
     */
    String[] requiredPermissions();
}
