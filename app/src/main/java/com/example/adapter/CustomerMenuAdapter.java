package com.example.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.R;
import com.example.model.MenuItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMenuAdapter extends RecyclerView.Adapter<CustomerMenuAdapter.MenuViewHolder> {

    public interface CartListener {
        void onCartChanged(Map<Integer, Integer> cartMap, BigDecimal totalAmount);
    }

    private final List<MenuItem> items = new ArrayList<>();
    private final Map<Integer, Integer> cartMap = new HashMap<>(); // itemId -> quantity
    private final CartListener cartListener;

    public CustomerMenuAdapter(CartListener cartListener) {
        this.cartListener = cartListener;
    }

    public void setItems(List<MenuItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public Map<Integer, Integer> getCartMap() {
        return cartMap;
    }

    public void clearCart() {
        cartMap.clear();
        notifyDataSetChanged();
        if (cartListener != null) {
            cartListener.onCartChanged(cartMap, BigDecimal.ZERO);
        }
    }

    public MenuItem getItemById(int itemId) {
        for (MenuItem item : items) {
            if (item.getItemId() == itemId) return item;
        }
        return null;
    }

    public Map<Integer, MenuItem> getItemLookup() {
        Map<Integer, MenuItem> lookup = new HashMap<>();
        for (MenuItem item : items) {
            lookup.put(item.getItemId(), item);
        }
        return lookup;
    }

    public void updateItemQuantity(int itemId, int newQty) {
        if (newQty <= 0) {
            cartMap.remove(itemId);
        } else {
            cartMap.put(itemId, newQty);
        }
        notifyDataSetChanged();
        notifyCartUpdated();
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_customer, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        MenuItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class MenuViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivItemImage;
        TextView tvItemName, tvItemCategory, tvItemDescription, tvItemPrice, tvQuantity, tvItemUnavailableBadge;
        MaterialButton btnAddToCart;
        LinearLayout layoutQuantityControls;
        ImageButton btnMinus, btnPlus;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.ivItemImage);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
            tvItemDescription = itemView.findViewById(R.id.tvItemDescription);
            tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
            tvQuantity = itemView.findViewById(R.id.tvQtyValue);
            tvItemUnavailableBadge = itemView.findViewById(R.id.tvItemUnavailableBadge);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
            layoutQuantityControls = itemView.findViewById(R.id.layoutQuantityControls);
            btnMinus = itemView.findViewById(R.id.btnQtyMinus);
            btnPlus = itemView.findViewById(R.id.btnQtyPlus);
        }

        public void bind(MenuItem item) {
            tvItemName.setText(item.getName());
            tvItemCategory.setText(item.getCategory());
            tvItemDescription.setText(item.getDescription());
            if (item.getPrice().compareTo(new BigDecimal("25")) < 0) {
                tvItemPrice.setText(String.format("$%.2f", item.getPrice()));
            } else {
                tvItemPrice.setText(String.format("₹%.2f", item.getPrice()));
            }

            // Load Image with Glide
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(ivItemImage);
            } else {
                ivItemImage.setImageResource(R.drawable.ic_launcher_background);
            }

            boolean isAvailable = item.isAvailable();
            if (!isAvailable) {
                itemView.setAlpha(0.60f);
                if (tvItemUnavailableBadge != null) {
                    tvItemUnavailableBadge.setVisibility(View.VISIBLE);
                }
                layoutQuantityControls.setVisibility(View.GONE);
                btnAddToCart.setVisibility(View.VISIBLE);
                btnAddToCart.setText("Sold Out");
                btnAddToCart.setEnabled(false);
                btnAddToCart.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#94A3B8")));
                if (cartMap.containsKey(item.getItemId())) {
                    cartMap.remove(item.getItemId());
                    notifyCartUpdated();
                }
            } else {
                itemView.setAlpha(1.0f);
                if (tvItemUnavailableBadge != null) {
                    tvItemUnavailableBadge.setVisibility(View.GONE);
                }
                btnAddToCart.setEnabled(true);
                btnAddToCart.setText("+ ADD");
                btnAddToCart.setBackgroundTintList(ColorStateList.valueOf(itemView.getContext().getResources().getColor(R.color.primary)));

                int qty = cartMap.containsKey(item.getItemId()) ? cartMap.get(item.getItemId()) : 0;
                updateQuantityUI(qty);
            }

            btnAddToCart.setOnClickListener(v -> {
                if (!item.isAvailable()) return;
                int newQty = 1;
                cartMap.put(item.getItemId(), newQty);
                updateQuantityUI(newQty);
                notifyCartUpdated();
            });

            btnPlus.setOnClickListener(v -> {
                if (!item.isAvailable()) return;
                int currentQty = cartMap.getOrDefault(item.getItemId(), 0);
                int newQty = currentQty + 1;
                cartMap.put(item.getItemId(), newQty);
                updateQuantityUI(newQty);
                notifyCartUpdated();
            });

            btnMinus.setOnClickListener(v -> {
                int currentQty = cartMap.getOrDefault(item.getItemId(), 0);
                if (currentQty <= 1) {
                    cartMap.remove(item.getItemId());
                    updateQuantityUI(0);
                } else {
                    int newQty = currentQty - 1;
                    cartMap.put(item.getItemId(), newQty);
                    updateQuantityUI(newQty);
                }
                notifyCartUpdated();
            });
        }

        private void updateQuantityUI(int qty) {
            if (qty > 0) {
                btnAddToCart.setVisibility(View.GONE);
                layoutQuantityControls.setVisibility(View.VISIBLE);
                tvQuantity.setText(String.valueOf(qty));
            } else {
                btnAddToCart.setVisibility(View.VISIBLE);
                layoutQuantityControls.setVisibility(View.GONE);
            }
        }
    }

    private void notifyCartUpdated() {
        if (cartListener != null) {
            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<Integer, Integer> entry : cartMap.entrySet()) {
                MenuItem item = getItemById(entry.getKey());
                if (item != null && entry.getValue() > 0) {
                    total = total.add(item.getPrice().multiply(new BigDecimal(entry.getValue())));
                }
            }
            cartListener.onCartChanged(cartMap, total);
        }
    }
}
