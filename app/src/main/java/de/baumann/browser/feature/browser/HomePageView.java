package de.baumann.browser.feature.browser;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import de.baumann.browser.R;
import de.baumann.browser.feature.common.BrowserUnit;
import de.baumann.browser.feature.common.HelperUnit;
import de.baumann.browser.feature.data.AdapterRecord;
import de.baumann.browser.feature.data.Record;
import de.baumann.browser.feature.data.RecordAction;

public class HomePageView extends FrameLayout {

    public interface HomePageCallback {
        void onUrlRequested(String url);
        void onNewTabRequested(String url);
        void onSearchRequested();
    }

    private final Context context;
    private HomePageCallback callback;
    private AdapterHomeShortcuts adapter;
    private RecyclerView recyclerView;

    public HomePageView(@NonNull Context context) {
        super(context);
        this.context = context;
        init();
    }

    public HomePageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public HomePageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    public void setCallback(HomePageCallback callback) {
        this.callback = callback;
    }

    private void init() {
        View root = LayoutInflater.from(context).inflate(R.layout.layout_home_page, this, true);

        MaterialCardView searchBar = root.findViewById(R.id.home_search_bar);
        searchBar.setOnClickListener(v -> {
            if (callback != null) {
                callback.onSearchRequested();
            }
        });

        recyclerView = root.findViewById(R.id.home_shortcuts_recycler);
        int spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 6 : 4;
        recyclerView.setLayoutManager(new GridLayoutManager(context, spanCount));

        adapter = new AdapterHomeShortcuts(context, new AdapterHomeShortcuts.ShortcutListener() {
            @Override
            public void onShortcutClick(Record record) {
                if (callback != null) {
                    callback.onUrlRequested(record.getURL());
                }
            }

            @Override
            public void onShortcutLongClick(Record record, View view) {
                showShortcutOptions(record);
            }

            @Override
            public void onAddClick() {
                openBookmarksPicker();
            }
        });

        recyclerView.setAdapter(adapter);
        refreshShortcuts();
    }

    public void refreshShortcuts() {
        RecordAction action = new RecordAction(context);
        List<Record> shortcuts = action.listStart();
        if (adapter != null) {
            adapter.setShortcuts(shortcuts);
        }
    }

    /**
     * "On plus click, Open Bookmarks, Choose one from it"
     */
    private void openBookmarksPicker() {
        RecordAction action = new RecordAction(context);
        action.open(false);
        List<Record> bookmarks = action.listBookmark(context, false, 0);
        action.close();

        if (bookmarks.isEmpty()) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.home_choose_bookmark)
                    .setIcon(R.drawable.icon_bookmark)
                    .setMessage(R.string.home_no_bookmarks)
                    .setPositiveButton(R.string.home_add_custom, (d, w) -> showAddCustomShortcutDialog())
                    .setNegativeButton(R.string.app_cancel, null)
                    .show();
        } else {
            AdapterRecord bookmarkAdapter = new AdapterRecord(context, bookmarks);
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.home_choose_bookmark)
                    .setIcon(R.drawable.icon_bookmark)
                    .setAdapter(bookmarkAdapter, (dialog, which) -> {
                        Record chosen = bookmarks.get(which);
                        action.addStart(new Record(chosen.getTitle(), chosen.getURL(), 0, 0));
                        refreshShortcuts();

                        Snackbar snackbar = Snackbar.make(this,
                                context.getString(R.string.home_added) + ": " + chosen.getTitle(),
                                Snackbar.LENGTH_LONG);
                        HelperUnit.makeSnackbarRound(snackbar);
                        snackbar.setAction(context.getString(R.string.app_open), v -> {
                            if (callback != null) {
                                callback.onUrlRequested(chosen.getURL());
                            }
                        });
                        snackbar.show();
                    })
                    .setNegativeButton(R.string.app_cancel, null)
                    .show();
        }
    }

    private void showShortcutOptions(Record record) {
        String[] options = new String[]{
                context.getString(R.string.home_open_new_tab),
                context.getString(R.string.home_remove_shortcut)
        };

        new MaterialAlertDialogBuilder(context)
                .setTitle(record.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (callback != null) {
                            callback.onNewTabRequested(record.getURL());
                        }
                    } else if (which == 1) {
                        RecordAction action = new RecordAction(context);
                        action.deleteStart(record.getURL());
                        refreshShortcuts();

                        Snackbar snackbar = Snackbar.make(this, R.string.home_removed, Snackbar.LENGTH_SHORT);
                        HelperUnit.makeSnackbarRound(snackbar);
                        snackbar.show();
                    }
                })
                .show();
    }

    private void showAddCustomShortcutDialog() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, padding / 2);

        EditText titleInput = new EditText(context);
        titleInput.setHint(R.string.dialog_title_hint);
        layout.addView(titleInput);

        EditText urlInput = new EditText(context);
        urlInput.setHint(R.string.dialog_URL_hint);
        layout.addView(urlInput);

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.home_add_custom)
                .setView(layout)
                .setPositiveButton(R.string.app_ok, (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String rawUrl = urlInput.getText().toString().trim();
                    if (!rawUrl.isEmpty()) {
                        String fullUrl = BrowserUnit.queryWrapper(context, rawUrl);
                        if (title.isEmpty()) {
                            title = HelperUnit.domain(fullUrl);
                        }
                        RecordAction action = new RecordAction(context);
                        action.addStart(new Record(title, fullUrl, 0, 0));
                        refreshShortcuts();
                    }
                })
                .setNegativeButton(R.string.app_cancel, null)
                .show();
    }
}
