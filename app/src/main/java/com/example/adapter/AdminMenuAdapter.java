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

    public AdminMenuAdapter(MenuAdminListener listener) {
        this.listener = listener;
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
        MaterialSwitch switchAvailability;
        ImageButton btnDeleteMenuItem;

        public AdminViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAdminItemImage = itemView.findViewById(R.id.ivAdminItemImage);
            tvAdminItemName = itemView.findViewById(R.id.tvAdminItemName);
            tvAdminItemCat = itemView.findViewById(R.id.tvAdminItemCat);
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

            // Temporarily detach listener to avoid triggering during recycle
            switchAvailability.setOnCheckedChangeListener(null);
            switchAvailability.setChecked(item.isAvailable());
            switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onAvailabilityToggled(item, isChecked);
                }
            });

            btnDeleteMenuItem.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemDeleteRequested(item);
                }
            });
        }
    }
}
