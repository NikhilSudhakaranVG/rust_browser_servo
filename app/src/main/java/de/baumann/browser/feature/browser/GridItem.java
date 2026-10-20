package de.baumann.browser.feature.browser;

public class GridItem {
    private final String title;
    private final int data;

    public GridItem (String title, int data) {
        this.title = title;
        this.data = data;
    }

    public String getTitle() {
        return title;
    }
    public int getData() {
        return data;
    }
}
