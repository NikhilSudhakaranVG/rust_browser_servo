package de.baumann.browser.feature.settings;
import de.baumann.browser.feature.dialogs.MenuItem;

import static android.view.View.GONE;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Objects;

import de.baumann.browser.R;

public class AdapterSettingsMenu extends RecyclerView.Adapter<AdapterSettingsMenu.ViewHolder> {

    private final List<MenuItem> itemList;

    public AdapterSettingsMenu(List<MenuItem> itemList) {
        this.itemList = itemList;
    }

    /**
     * Notifies only the items whose selection state has changed, using
     * {@link DiffUtil} to avoid full redraws.
     *
     * @param newList the updated list (must be the same object as {@code itemList}
     *                after mutations, or a replacement list with equal size/order)
     */
    public void updateItems(List<MenuItem> newList) {
        List<MenuItem> oldSnapshot = List.copyOf(itemList);

        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return oldSnapshot.size(); }
            @Override public int getNewListSize() { return newList.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return Objects.equals(oldSnapshot.get(oldPos).getTitle(), newList.get(newPos).getTitle());
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                MenuItem o = oldSnapshot.get(oldPos);
                MenuItem n = newList.get(newPos);
                return o.isSelected() == n.isSelected()
                        && Objects.equals(o.getTitle(), n.getTitle())
                        && o.getIconResId() == n.getIconResId();
            }
        });

        itemList.clear();
        itemList.addAll(newList);
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_checkbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = itemList.get(position);
        Context context = holder.itemView.getContext();

        holder.textView.setText(item.getTitle());
        holder.imageView.setImageResource(item.getIconResId());

        // 1. WICHTIG: Eventuelle alte Listener entfernen, bevor der Status gesetzt wird
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.itemView.setOnClickListener(null);

        // 2. Visuellen Status setzen (theme-aware colors instead of hardcoded values)
        holder.checkBox.setChecked(item.isSelected());
        applyCardColor(holder.cardView, context, item.isSelected());

        // 3. Listener für die Checkbox (falls direkt auf das Häkchen getippt wird)
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setSelected(isChecked);
            applyCardColor(holder.cardView, context, isChecked);
        });

        // 4. Listener für die gesamte Zeile (schaltet den Status um)
        holder.itemView.setOnClickListener(v -> {
            boolean nextState = !item.isSelected();
            item.setSelected(nextState);
            // Nutzt die aktuelle Position des ViewHolders für ein sauberes UI-Update
            notifyItemChanged(holder.getBindingAdapterPosition());
        });
    }

    /**
     * Applies a theme-aware background to the card depending on its selected state.
     * Uses {@code colorSecondaryContainer} when selected and
     * {@code colorSurfaceContainer} when not selected — both respect dark/light mode.
     */
    private static void applyCardColor(CardView card, Context context, boolean selected) {
        TypedValue tv = new TypedValue();
        int attr = selected
                ? com.google.android.material.R.attr.colorSecondaryContainer
                : com.google.android.material.R.attr.colorSurfaceContainer;
        context.getTheme().resolveAttribute(attr, tv, true);
        card.setCardBackgroundColor(tv.data);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;
        final ImageView imageView;
        final CheckBox checkBox;
        final CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.titleView);
            imageView = itemView.findViewById(R.id.item_icon);
            checkBox = itemView.findViewById(R.id.item_checkBox);
            cardView = itemView.findViewById(R.id.item_cardView);

            TextView tv = itemView.findViewById(R.id.dateView);
            tv.setVisibility(GONE);
        }
    }
}
