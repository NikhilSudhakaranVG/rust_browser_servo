package de.baumann.browser.feature.browser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BrowserContainer {
    private static final List<AlbumController> list = new LinkedList<>();

    public synchronized static AlbumController get(int index) {
        if (index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    public synchronized static void add(AlbumController controller) {
        if (controller != null && !list.contains(controller)) {
            list.add(controller);
        }
    }

    public synchronized static void add(AlbumController controller, int index) {
        if (controller != null && !list.contains(controller)) {
            if (index >= 0 && index <= list.size()) {
                list.add(index, controller);
            } else {
                list.add(controller);
            }
        }
    }

    public synchronized static void remove(AlbumController controller) {
        if (controller == null) return;
        if (controller instanceof NinjaWebView) {
            ((NinjaWebView) controller).destroy();
        }
        list.remove(controller);
    }

    public synchronized static int indexOf(AlbumController controller) {
        return list.indexOf(controller);
    }

    public synchronized static List<AlbumController> list() {
        return new ArrayList<>(list);
    }

    public synchronized static int size() {
        return list.size();
    }

    public synchronized static void clear() {
        for (AlbumController albumController : list) {
            if (albumController instanceof NinjaWebView) {
                ((NinjaWebView) albumController).destroy();
            }
        }
        list.clear();
    }

    public synchronized static boolean hasIncognito() {
        for (AlbumController controller : list) {
            if (controller != null && controller.isIncognito()) {
                return true;
            }
        }
        return false;
    }

    public synchronized static int countIncognito() {
        int count = 0;
        for (AlbumController controller : list) {
            if (controller != null && controller.isIncognito()) {
                count++;
            }
        }
        return count;
    }
}
