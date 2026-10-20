package de.baumann.browser.feature.browser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ActivityCustomSearches extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String sharedText = intent != null ? intent.getStringExtra(Intent.EXTRA_TEXT) : null;
        Intent browserIntent = new Intent(this, BrowserActivity.class);
        if (sharedText != null) {
            browserIntent.putExtra(Intent.EXTRA_TEXT, sharedText);
        }
        browserIntent.setAction("customSearches");
        startActivity(browserIntent);
        finish();
    }
}