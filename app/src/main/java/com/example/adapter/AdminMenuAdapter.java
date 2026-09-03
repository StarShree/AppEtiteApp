package com.example.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.R;
import com.example.model.MenuItem;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class AdminMenuAdapter extends RecyclerView.Adapter<AdminMenuAdapter.AdminViewHolder> {

    public interface MenuAdminListener {
        void onAvailabilityToggled(MenuItem item, boolean isAvailable);
        void onItemDeleteRequested(MenuItem item);
    }

    private final List<MenuItem> items = new ArrayList<>();
    private final MenuAdminListener listener;
    private boolean allowDelete = true;

    public AdminMenuAdapter(MenuAdminListener listener) {
        this.listener = listener;
        this.allowDelete = true;
    }

    public AdminMenuAdapter(MenuAdminListener listener, boolean allowDelete) {
        this.listener = listener;
        this.allowDelete = allowDelete;
    }

    public void setAllowDelete(boolean allowDelete) {
        this.allowDelete = allowDelete;
        notifyDataSetChanged();
    }

    public void setItems(List<MenuItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_menu, parent, false);
        return new AdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
        MenuItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class AdminViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAdminItemImage;
        TextView tvAdminItemName, tvAdminItemCat;
        TextView tvAdminAvailabilityStatus, tvAdminAvailabilitySubtitle;
        MaterialSwitch switchAvailability;
        ImageButton btnDeleteMenuItem;

        public AdminViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAdminItemImage = itemView.findViewById(R.id.ivAdminItemImage);
            tvAdminItemName = itemView.findViewById(R.id.tvAdminItemName);
            tvAdminItemCat = itemView.findViewById(R.id.tvAdminItemCat);
            tvAdminAvailabilityStatus = itemView.findViewById(R.id.tvAdminAvailabilityStatus);
            tvAdminAvailabilitySubtitle = itemView.findViewById(R.id.tvAdminAvailabilitySubtitle);
            switchAvailability = itemView.findViewById(R.id.switchAvailability);
            btnDeleteMenuItem = itemView.findViewById(R.id.btnDeleteMenuItem);
        }

        public void bind(MenuItem item) {
            tvAdminItemName.setText(item.getName());
            tvAdminItemCat.setText(String.format("%s • ₹%.2f", item.getCategory(), item.getPrice()));

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(ivAdminItemImage);
            } else {
                ivAdminItemImage.setImageResource(R.drawable.ic_launcher_background);
            }

            btnDeleteMenuItem.setVisibility(allowDelete ? View.VISIBLE : View.GONE);
            btnDeleteMenuItem.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemDeleteRequested(item);
                }
            });

            updateStatusUi(item.isAvailable());

            // Temporarily detach listener to avoid triggering during recycle
            switchAvailability.setOnCheckedChangeListener(null);
            switchAvailability.setChecked(item.isAvailable());
            switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setAvailable(isChecked);
                updateStatusUi(isChecked);
                if (listener != null) {
                    listener.onAvailabilityToggled(item, isChecked);
                }
            });
        }

        private void updateStatusUi(boolean isAvailable) {
            if (isAvailable) {
                itemView.setAlpha(1.0f);
                if (tvAdminAvailabilityStatus != null) {
                    tvAdminAvailabilityStatus.setText("🟢 In Stock & Available");
                    tvAdminAvailabilityStatus.setTextColor(android.graphics.Color.parseColor("#10B981"));
                }
                if (tvAdminAvailabilitySubtitle != null) {
                    tvAdminAvailabilitySubtitle.setText("Live on student menu & orderable");
                }
            } else {
                itemView.setAlpha(0.72f);
                if (tvAdminAvailabilityStatus != null) {
                    tvAdminAvailabilityStatus.setText("🔴 Unavailable (Sold Out)");
                    tvAdminAvailabilityStatus.setTextColor(android.graphics.Color.parseColor("#EF4444"));
                }
                if (tvAdminAvailabilitySubtitle != null) {
                    tvAdminAvailabilitySubtitle.setText("Marked sold out • Ordering blocked for students");
                }
            }
        }
    }
}
