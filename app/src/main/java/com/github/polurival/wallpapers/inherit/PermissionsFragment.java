package com.github.polurival.wallpapers.inherit;

/**
 * Для запроса разрешений для фрагментов
 */

public interface PermissionsFragment {

    /**
     * @return массив всех разрешений, необходимых для этого фрагмента
     */
    String[] requiredPermissions();
}
