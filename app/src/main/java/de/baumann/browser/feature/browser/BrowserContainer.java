package de.baumann.browser.feature.browser;

import java.util.LinkedList;
import java.util.List;


public class BrowserContainer {
    private static final List<AlbumController> list = new LinkedList<>();
    public static AlbumController get(int index) {
        return list.get(index);
    }
    public synchronized static void add(AlbumController controller) {
        list.add(controller);
    }
    public synchronized static void add(AlbumController controller, int index) {list.add(index, controller);}
    public synchronized static void remove(AlbumController controller) {
        ((NinjaWebView) controller).destroy();
        list.remove(controller);
    }
    public static int indexOf(AlbumController controller) {return list.indexOf(controller);}
    public static List<AlbumController> list() {return list;}
    public static int size() {
        return list.size();
    }
    public synchronized static void clear() {
        for (AlbumController albumController : list) {
            ((NinjaWebView) albumController).destroy();
        }
        list.clear();
    }
    public static boolean hasIncognito() {
        for (AlbumController controller : list) {
            if (controller.isIncognito()) {
                return true;
            }
        }
        return false;
    }
    public static int countIncognito() {
        int count = 0;
        for (AlbumController controller : list) {
            if (controller.isIncognito()) {
                count++;
            }
        }
        return count;
    }
}
