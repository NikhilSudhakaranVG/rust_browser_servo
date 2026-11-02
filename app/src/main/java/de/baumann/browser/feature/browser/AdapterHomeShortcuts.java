package de.baumann.browser.feature.browser;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.feature.data.FaviconHelper;
import de.baumann.browser.feature.data.Record;

import android.util.LruCache;

public class AdapterHomeShortcuts extends RecyclerView.Adapter<AdapterHomeShortcuts.ViewHolder> {

    private static final int VIEW_TYPE_SHORTCUT = 0;
    private static final int VIEW_TYPE_ADD = 1;
    private static final LruCache<String, Bitmap> sFaviconCache = new LruCache<>(64);

    public interface ShortcutListener {
        void onShortcutClick(Record record);
        void onShortcutLongClick(Record record, View view);
        void onAddClick();
    }

    private final Context context;
    private final List<Record> shortcuts = new ArrayList<>();
    private final ShortcutListener listener;

    public AdapterHomeShortcuts(Context context, ShortcutListener listener) {
        this.context = context;
        this.listener = listener;
        setHasStableIds(true);
    }

    /**
     * Updates the shortcut list using {@link DiffUtil} for efficient, animated
     * updates instead of a full {@code notifyDataSetChanged()}.
     */
    public void setShortcuts(@Nullable List<Record> newList) {
        List<Record> oldList = new ArrayList<>(shortcuts);
        List<Record> safeNewList = (newList != null) ? newList : new ArrayList<>();

        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return oldList.size(); }
            @Override public int getNewListSize() { return safeNewList.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                // Use URL as the stable identity of a shortcut
                return Objects.equals(oldList.get(oldPos).getURL(), safeNewList.get(newPos).getURL());
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                Record o = oldList.get(oldPos);
                Record n = safeNewList.get(newPos);
                return Objects.equals(o.getURL(), n.getURL())
                        && Objects.equals(o.getTitle(), n.getTitle());
            }
        });

        shortcuts.clear();
        shortcuts.addAll(safeNewList);
        result.dispatchUpdatesTo(this);
    }

    @Override
    public long getItemId(int position) {
        if (position < shortcuts.size()) {
            String url = shortcuts.get(position).getURL();
            return url != null ? url.hashCode() : RecyclerView.NO_ID;
        }
        // The "Add" tile has a fixed synthetic id
        return Long.MIN_VALUE;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < shortcuts.size()) {
            return VIEW_TYPE_SHORTCUT;
        }
        return VIEW_TYPE_ADD;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_home_shortcut, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TypedValue typedValue = new TypedValue();

        if (getItemViewType(position) == VIEW_TYPE_SHORTCUT) {
            Record item = shortcuts.get(position);
            holder.title.setText(item.getTitle());

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainer, typedValue, true);
            holder.card.setCardBackgroundColor(typedValue.data);

            // Accessibility: describe the card as a shortcut to the site
            String desc = item.getTitle() != null ? item.getTitle() : "";
            holder.card.setContentDescription(context.getString(R.string.home_shortcut_description, desc));

            String url = item.getURL();
            Bitmap cachedBitmap = (url != null) ? sFaviconCache.get(url) : null;
            if (cachedBitmap != null) {
                holder.icon.setImageBitmap(cachedBitmap);
                holder.icon.setVisibility(View.VISIBLE);
                holder.letter.setVisibility(View.GONE);
            } else {
                Bitmap loadedBitmap = null;
                if (url != null) {
                    try (FaviconHelper faviconHelper = new FaviconHelper(context)) {
                        loadedBitmap = faviconHelper.getFavicon(url);
                        if (loadedBitmap != null) {
                            sFaviconCache.put(url, loadedBitmap);
                        }
                    } catch (Exception ignored) {}
                }

                if (loadedBitmap != null) {
                    holder.icon.setImageBitmap(loadedBitmap);
                    holder.icon.setVisibility(View.VISIBLE);
                    holder.letter.setVisibility(View.GONE);
                } else {
                    String firstLetter = "";
                    if (item.getTitle() != null && !item.getTitle().trim().isEmpty()) {
                        firstLetter = item.getTitle().trim().substring(0, 1).toUpperCase(Locale.getDefault());
                    }
                    if (!firstLetter.isEmpty()) {
                        holder.icon.setVisibility(View.GONE);
                        holder.letter.setText(firstLetter);
                        holder.letter.setVisibility(View.VISIBLE);
                    } else {
                        holder.icon.setImageResource(R.drawable.icon_web);
                        holder.icon.setVisibility(View.VISIBLE);
                        holder.letter.setVisibility(View.GONE);
                    }
                }
            }

            holder.card.setOnClickListener(v -> {
                if (listener != null) listener.onShortcutClick(item);
            });

            holder.card.setOnLongClickListener(v -> {
                if (listener != null) listener.onShortcutLongClick(item, v);
                return true;
            });

        } else {
            // Plus / Add button tile
            holder.title.setText(context.getString(R.string.home_add_shortcut));

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHighest, typedValue, true);
            holder.card.setCardBackgroundColor(typedValue.data);

            holder.card.setContentDescription(context.getString(R.string.home_add_shortcut));

            holder.icon.setImageResource(R.drawable.icon_add);
            holder.icon.setVisibility(View.VISIBLE);
            holder.letter.setVisibility(View.GONE);

            holder.card.setOnClickListener(v -> {
                if (listener != null) listener.onAddClick();
            });

            holder.card.setOnLongClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return shortcuts.size() + 1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView icon;
        TextView letter;
        TextView title;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.shortcut_card);
            icon = itemView.findViewById(R.id.shortcut_icon);
            letter = itemView.findViewById(R.id.shortcut_letter);
            title = itemView.findViewById(R.id.shortcut_title);
        }
    }
}
