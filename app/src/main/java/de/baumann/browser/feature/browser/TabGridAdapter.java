package de.baumann.browser.feature.browser;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.baumann.browser.R;
import de.baumann.browser.feature.common.BrowserUnit;
import de.baumann.browser.feature.common.HelperUnit;
import de.baumann.browser.feature.data.FaviconHelper;

public class TabGridAdapter extends RecyclerView.Adapter<TabGridAdapter.TabViewHolder> {

    public interface OnTabClickListener {
        void onTabSelected(AlbumController controller, int position);
        void onTabClosed(AlbumController controller, int position);
        void onTabLongClick(AlbumController controller, View view, int position);
    }

    private final Context context;
    private final List<AlbumController> items = new ArrayList<>();
    private AlbumController activeTab;
    private final OnTabClickListener listener;

    public TabGridAdapter(@NonNull Context context, @NonNull OnTabClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setTabs(List<AlbumController> newTabs, AlbumController activeTab) {
        this.items.clear();
        if (newTabs != null) {
            this.items.addAll(newTabs);
        }
        this.activeTab = activeTab;
        notifyDataSetChanged();
    }

    public AlbumController getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    public void removeTabAt(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void insertTabAt(AlbumController controller, int position) {
        if (controller != null) {
            int safeIndex = Math.max(0, Math.min(position, items.size()));
            items.add(safeIndex, controller);
            notifyItemInserted(safeIndex);
        }
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tab_card, parent, false);
        return new TabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        AlbumController controller = items.get(position);
        if (controller == null) return;

        boolean isIncognito = controller.isIncognito();
        boolean isActive = (controller == activeTab || controller.isForeground());
        String url = controller.getUrl();
        if (url == null) url = "";
        String domain = HelperUnit.domain(url);

        // ── 1. Title Resolution ──
        String title = controller.getTitle();
        if (isIncognito && (title == null || title.isEmpty() || title.equals(context.getString(R.string.home)))) {
            title = context.getString(R.string.incognito_tab);
        } else if (title == null || title.isEmpty()) {
            title = context.getString(R.string.home);
        }
        holder.tabTitle.setText(title);

        // ── 2. Theme Attribute Colors ──
        TypedValue tvPrimary = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, tvPrimary, true);
        int colorPrimary = tvPrimary.data;

        TypedValue tvOutlineVariant = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, tvOutlineVariant, true);
        int colorOutlineVariant = tvOutlineVariant.data;

        TypedValue tvSurfaceContainer = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainer, tvSurfaceContainer, true);
        int colorSurfaceContainer = tvSurfaceContainer.data;

        TypedValue tvSurfaceContainerHigh = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHigh, tvSurfaceContainerHigh, true);
        int colorSurfaceContainerHigh = tvSurfaceContainerHigh.data;

        TypedValue tvSurfaceContainerLow = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerLow, tvSurfaceContainerLow, true);
        int colorSurfaceContainerLow = tvSurfaceContainerLow.data;

        TypedValue tvOnSurface = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tvOnSurface, true);
        int colorOnSurface = tvOnSurface.data;

        // ── 3. Incognito vs Normal Container Styling ──
        int colorIncognitoAccent = ContextCompat.getColor(context, R.color.incognito_accent);
        int colorIncognitoActive = ContextCompat.getColor(context, R.color.incognito_tab_active);
        int colorIncognitoInactive = ContextCompat.getColor(context, R.color.incognito_tab_inactive);
        int colorIncognitoSurface = ContextCompat.getColor(context, R.color.incognito_surface);
        int colorIncognitoOnSurface = ContextCompat.getColor(context, R.color.incognito_on_surface);

        if (isIncognito) {
            holder.tabCard.setCardBackgroundColor(isActive ? colorIncognitoActive : colorIncognitoInactive);
            holder.tabCardHeader.setBackgroundColor(colorIncognitoSurface);
            holder.tabTitle.setTextColor(colorIncognitoOnSurface);
            holder.tabIncognitoBadge.setVisibility(View.VISIBLE);
            holder.tabPreviewContainer.setBackgroundColor(colorIncognitoSurface);
            holder.tabBtnClose.setColorFilter(Color.parseColor("#A0AEC0"));
        } else {
            holder.tabCard.setCardBackgroundColor(colorSurfaceContainer);
            holder.tabCardHeader.setBackgroundColor(colorSurfaceContainerHigh);
            holder.tabTitle.setTextColor(colorOnSurface);
            holder.tabIncognitoBadge.setVisibility(View.GONE);
            holder.tabPreviewContainer.setBackgroundColor(colorSurfaceContainerLow);
            holder.tabBtnClose.clearColorFilter();
        }

        // ── 4. Active Tab Highlight Border ──
        if (isActive) {
            holder.tabCard.setStrokeColor(isIncognito ? colorIncognitoAccent : colorPrimary);
            holder.tabCard.setStrokeWidth(HelperUnit.convertDpToPixel(2.5f, context));
            holder.tabCard.setCardElevation(HelperUnit.convertDpToPixel(4f, context));
        } else {
            holder.tabCard.setStrokeColor(isIncognito ? Color.parseColor("#4A4458") : colorOutlineVariant);
            holder.tabCard.setStrokeWidth(HelperUnit.convertDpToPixel(1f, context));
            holder.tabCard.setCardElevation(HelperUnit.convertDpToPixel(1f, context));
        }

        // ── 5. Favicon Binding ──
        if (isIncognito) {
            holder.tabFavicon.setImageResource(R.drawable.icon_incognito);
            holder.tabFavicon.setColorFilter(colorIncognitoAccent);
        } else if (BrowserUnit.URL_HOME.equals(url) || "foss://home".equals(url)) {
            holder.tabFavicon.setImageResource(R.drawable.icon_home_nav);
            holder.tabFavicon.setColorFilter(colorPrimary);
        } else {
            Bitmap fav = controller.getFavicon();
            if (fav != null && !fav.isRecycled()) {
                holder.tabFavicon.clearColorFilter();
                holder.tabFavicon.setImageBitmap(fav);
            } else {
                holder.tabFavicon.clearColorFilter();
                FaviconHelper.setFavicon(context, holder.tabFavicon, url, R.drawable.icon_web);
            }
        }

        // ── 6. Thumbnail Preview vs Fallback Placeholder ──
        Bitmap thumb = controller.getThumbnail();
        if (!isIncognito && thumb != null && !thumb.isRecycled()) {
            holder.tabThumbnail.setImageBitmap(thumb);
            holder.tabThumbnail.setVisibility(View.VISIBLE);
            holder.tabPlaceholder.setVisibility(View.GONE);
        } else {
            holder.tabThumbnail.setVisibility(View.GONE);
            holder.tabPlaceholder.setVisibility(View.VISIBLE);

            if (isIncognito) {
                holder.tabAvatarLetter.setVisibility(View.GONE);
                holder.tabAvatarIcon.setVisibility(View.VISIBLE);
                holder.tabAvatarIcon.setImageResource(R.drawable.icon_incognito);
                holder.tabAvatarIcon.setColorFilter(colorIncognitoAccent);
                holder.tabDomain.setText(R.string.incognito_tab);
                holder.tabDomain.setTextColor(colorIncognitoOnSurface);
                holder.tabAvatarCard.setCardBackgroundColor(Color.parseColor("#383244"));
            } else if (BrowserUnit.URL_HOME.equals(url) || "foss://home".equals(url)) {
                holder.tabAvatarLetter.setVisibility(View.GONE);
                holder.tabAvatarIcon.setVisibility(View.VISIBLE);
                holder.tabAvatarIcon.setImageResource(R.drawable.icon_home_nav);
                holder.tabAvatarIcon.setColorFilter(colorPrimary);
                holder.tabDomain.setText(R.string.home);
            } else {
                holder.tabAvatarIcon.setVisibility(View.GONE);
                holder.tabAvatarLetter.setVisibility(View.VISIBLE);
                String letter = domain.isEmpty() ? "W" : domain.substring(0, 1).toUpperCase(Locale.ROOT);
                holder.tabAvatarLetter.setText(letter);
                holder.tabDomain.setText(domain.isEmpty() ? title : domain);
            }
        }

        // ── 7. Click & Long-Click Listeners ──
        holder.tabCard.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                listener.onTabSelected(items.get(currentPos), currentPos);
            }
        });

        holder.tabBtnClose.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                listener.onTabClosed(items.get(currentPos), currentPos);
            }
        });

        holder.tabCard.setOnLongClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                listener.onTabLongClick(items.get(currentPos), holder.tabCard, currentPos);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class TabViewHolder extends RecyclerView.ViewHolder {
        public final MaterialCardView tabCard;
        public final LinearLayout tabCardHeader;
        public final ImageView tabFavicon;
        public final TextView tabTitle;
        public final ImageView tabBtnClose;
        public final View tabDivider;
        public final FrameLayout tabPreviewContainer;
        public final ImageView tabThumbnail;
        public final LinearLayout tabPlaceholder;
        public final MaterialCardView tabAvatarCard;
        public final TextView tabAvatarLetter;
        public final ImageView tabAvatarIcon;
        public final TextView tabDomain;
        public final MaterialCardView tabIncognitoBadge;

        public TabViewHolder(@NonNull View itemView) {
            super(itemView);
            tabCard = itemView.findViewById(R.id.tab_card);
            tabCardHeader = itemView.findViewById(R.id.tab_card_header);
            tabFavicon = itemView.findViewById(R.id.tab_favicon);
            tabTitle = itemView.findViewById(R.id.tab_title);
            tabBtnClose = itemView.findViewById(R.id.tab_btn_close);
            tabDivider = itemView.findViewById(R.id.tab_divider);
            tabPreviewContainer = itemView.findViewById(R.id.tab_preview_container);
            tabThumbnail = itemView.findViewById(R.id.tab_thumbnail);
            tabPlaceholder = itemView.findViewById(R.id.tab_placeholder);
            tabAvatarCard = itemView.findViewById(R.id.tab_avatar_card);
            tabAvatarLetter = itemView.findViewById(R.id.tab_avatar_letter);
            tabAvatarIcon = itemView.findViewById(R.id.tab_avatar_icon);
            tabDomain = itemView.findViewById(R.id.tab_domain);
            tabIncognitoBadge = itemView.findViewById(R.id.tab_incognito_badge);
        }
    }
}
