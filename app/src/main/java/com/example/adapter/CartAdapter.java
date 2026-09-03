package com.example.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.MenuItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface CartActionListener {
        void onQuantityChanged(MenuItem item, int newQty);
    }

    public static class CartEntry {
        public final MenuItem item;
        public int quantity;

        public CartEntry(MenuItem item, int quantity) {
            this.item = item;
            this.quantity = quantity;
        }
    }

    private final List<CartEntry> entries = new ArrayList<>();
    private final CartActionListener listener;

    public CartAdapter(CartActionListener listener) {
        this.listener = listener;
    }

    public void setCartData(Map<Integer, Integer> cartMap, Map<Integer, MenuItem> itemLookup) {
        entries.clear();
        if (cartMap != null && itemLookup != null) {
            for (Map.Entry<Integer, Integer> e : cartMap.entrySet()) {
                if (e.getValue() > 0 && itemLookup.containsKey(e.getKey())) {
                    entries.add(new CartEntry(itemLookup.get(e.getKey()), e.getValue()));
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart_entry, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartEntry entry = entries.get(position);
        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvCartItemName, tvCartItemPrice, tvCartItemQty, tvCartItemSubtotal;
        ImageButton btnCartMinus, btnCartPlus;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCartItemName = itemView.findViewById(R.id.tvCartItemName);
            tvCartItemPrice = itemView.findViewById(R.id.tvCartItemPrice);
            tvCartItemQty = itemView.findViewById(R.id.tvCartItemQty);
            tvCartItemSubtotal = itemView.findViewById(R.id.tvCartItemSubtotal);
            btnCartMinus = itemView.findViewById(R.id.btnCartMinus);
            btnCartPlus = itemView.findViewById(R.id.btnCartPlus);
        }

        public void bind(CartEntry entry) {
            tvCartItemName.setText(entry.item.getName());
            tvCartItemPrice.setText(String.format("₹%.2f each", entry.item.getPrice()));
            tvCartItemQty.setText(String.valueOf(entry.quantity));

            BigDecimal subtotal = entry.item.getPrice().multiply(BigDecimal.valueOf(entry.quantity));
            tvCartItemSubtotal.setText(String.format("₹%.2f", subtotal));

            btnCartMinus.setOnClickListener(v -> {
                int newQty = entry.quantity - 1;
                if (listener != null) {
                    listener.onQuantityChanged(entry.item, newQty);
                }
            });

            btnCartPlus.setOnClickListener(v -> {
                int newQty = entry.quantity + 1;
                if (listener != null) {
                    listener.onQuantityChanged(entry.item, newQty);
                }
            });
        }
    }
}
