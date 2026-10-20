package de.baumann.browser.feature.browser;
import de.baumann.browser.feature.common.HelperUnit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import de.baumann.browser.R;

public class AdapterTabs {

    private final Context context;
    private final AlbumController albumController;
    private View albumView;
    private TextView albumTitle;
    private TextView albumUrl;
    private ImageView albumIcon;
    private BrowserController browserController;
    private MaterialCardView albumCardView;
    private boolean foreground = false;
    private boolean isIncognito = false;

    AdapterTabs(Context context, AlbumController albumController, BrowserController browserController) {
        this.context = context;
        this.albumController = albumController;
        this.browserController = browserController;
        initUI();
    }

    public void setIncognito(boolean incognito) {
        this.isIncognito = incognito;
        if (albumIcon != null) {
            albumIcon.setImageResource(incognito ? R.drawable.icon_incognito : R.drawable.icon_web);
        }
        if (foreground) {
            activate();
        } else {
            deactivate();
        }
    }

    public boolean isIncognito() {
        return isIncognito;
    }

    View getAlbumView() {
        return albumView;
    }

    /** Returns true if this tab is currently the foreground (active) tab. */
    public boolean isForeground() {
        return foreground;
    }

    public Object getUrl() {
        return albumUrl.getText().toString();
    }

    void setAlbumTitle(String title, String url) {
        if (isIncognito && (title == null || title.isEmpty() || title.equals(context.getString(R.string.home)))) {
            albumTitle.setText(context.getString(R.string.incognito_tab));
        } else {
            albumTitle.setText(title);
        }
        albumUrl.setText(url);
        HelperUnit.setHighLightedText(context, albumUrl, url, HelperUnit.domain(url));
        if (albumIcon != null) {
            albumIcon.setImageResource(isIncognito ? R.drawable.icon_incognito : R.drawable.icon_web);
        }
    }

    void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
    }

    @SuppressLint("InflateParams")
    private void initUI() {
        albumView = LayoutInflater.from(context).inflate(R.layout.item_list, null, false);
        albumCardView = albumView.findViewById(R.id.item_CardViewItem);
        albumTitle = albumView.findViewById(R.id.titleView);
        albumUrl = albumView.findViewById(R.id.dateView);
        albumIcon = albumView.findViewById(R.id.item_icon);
        if (albumIcon != null) {
            albumIcon.setImageResource(isIncognito ? R.drawable.icon_incognito : R.drawable.icon_web);
        }
        ImageView albumClose = albumView.findViewById(R.id.iconView);
        albumClose.setImageResource(R.drawable.icon_tab_remove);
        albumClose.setVisibility(View.VISIBLE);
        albumClose.setOnClickListener(view -> {
            browserController.removeAlbum(albumController);
            if (BrowserContainer.size() < 2) {
                browserController.hideOverview();
            }
        });
        assert albumCardView != null;
        albumView.setOnLongClickListener(v -> {
            browserController.showOverflow(null, albumCardView, 1, albumTitle.getText().toString(), albumUrl.getText().toString(), null, null, 0);
            return true;
        });
    }

    public void activate() {
        foreground = true;
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryInverse, typedValue, true);
        int color = isIncognito ? ContextCompat.getColor(context, R.color.incognito_tab_active) : typedValue.data;
        albumCardView.setCardBackgroundColor(color);
        albumTitle.setTypeface(null, Typeface.BOLD);
        albumView.setOnClickListener(view -> {
            albumCardView.setCardBackgroundColor(color);
            browserController.hideOverview();
        });
    }

    void deactivate() {
        foreground = false;
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHighest, typedValue, true);
        int color = isIncognito ? ContextCompat.getColor(context, R.color.incognito_tab_inactive) : typedValue.data;
        albumCardView.setCardBackgroundColor(color);
        albumTitle.setTypeface(null, Typeface.NORMAL);
        albumView.setOnClickListener(view -> {
            browserController.showAlbum(albumController);
            browserController.hideOverview();
        });
    }
}