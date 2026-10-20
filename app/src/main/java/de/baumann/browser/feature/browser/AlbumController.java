package de.baumann.browser.feature.browser;

import android.view.View;

public interface AlbumController {
    View getAlbumView();
    void activate();
    void deactivate();
    default boolean isIncognito() {
        return false;
    }
}
