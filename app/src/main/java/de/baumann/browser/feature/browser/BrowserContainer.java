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

    /**
     * Removes a controller from the container.
     *
     * <p>This method deliberately does <em>not</em> destroy the underlying
     * {@link NinjaWebView}. Destruction is a separate, explicit step, because
     * a removed tab can still be re-added (undo of a tab swipe/close). Callers
     * that really close a tab must call {@link NinjaWebView#destroy()}
     * themselves.</p>
     */
    public synchronized static void remove(AlbumController controller) {
        if (controller == null) return;
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

    public synchronized static int countNormal() {
        int count = 0;
        for (AlbumController controller : list) {
            if (controller != null && !controller.isIncognito()) {
                count++;
            }
        }
        return count;
    }

    public synchronized static List<AlbumController> listNormal() {
        List<AlbumController> result = new ArrayList<>();
        for (AlbumController controller : list) {
            if (controller != null && !controller.isIncognito()) {
                result.add(controller);
            }
        }
        return result;
    }

    public synchronized static List<AlbumController> listIncognito() {
        List<AlbumController> result = new ArrayList<>();
        for (AlbumController controller : list) {
            if (controller != null && controller.isIncognito()) {
                result.add(controller);
            }
        }
        return result;
    }
}
