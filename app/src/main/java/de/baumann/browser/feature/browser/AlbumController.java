package de.baumann.browser.feature.browser;

import android.graphics.Bitmap;
import android.view.View;

public interface AlbumController {
    View getAlbumView();
    void activate();
    void deactivate();
    default boolean isIncognito() {
        return false;
    }
    default Bitmap getThumbnail() {
        return null;
    }
    default void captureThumbnail() {}
    default String getTitle() {
        return "";
    }
    default String getUrl() {
        return "";
    }
    default Bitmap getFavicon() {
        return null;
    }
    default boolean isForeground() {
        return false;
    }
    default AlbumController getPredecessor() {
        return null;
    }
    default void setPredecessor(AlbumController controller) {}
}

