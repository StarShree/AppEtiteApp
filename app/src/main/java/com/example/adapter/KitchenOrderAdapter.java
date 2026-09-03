package com.example.adapter;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.Order;
import com.example.model.OrderItem;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class KitchenOrderAdapter extends RecyclerView.Adapter<KitchenOrderAdapter.OrderViewHolder> {

    public interface OnOrderStatusChangeListener {
        void onStatusChangeRequested(Order order, String newStatus);
    }

    private final List<Order> orders = new ArrayList<>();
    private final OnOrderStatusChangeListener listener;
    private final boolean isKitchenInteractive;

    public KitchenOrderAdapter(OnOrderStatusChangeListener listener, boolean isKitchenInteractive) {
        this.listener = listener;
        this.isKitchenInteractive = isKitchenInteractive;
    }

    public void setOrders(List<Order> newOrders) {
        this.orders.clear();
        if (newOrders != null) {
            this.orders.addAll(newOrders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kitchen_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderToken, tvOrderPin, tvOrderStatus, tvCustomerName, tvOrderTime, tvOrderItems, tvOrderTotal;
        MaterialButton btnNextStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderToken = itemView.findViewById(R.id.tvOrderToken);
            tvOrderPin = itemView.findViewById(R.id.tvOrderPin);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvOrderTime = itemView.findViewById(R.id.tvOrderTime);
            tvOrderItems = itemView.findViewById(R.id.tvOrderItems);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            btnNextStatus = itemView.findViewById(R.id.btnNextStatus);
        }

        public void bind(Order order) {
            tvOrderToken.setText(order.getTokenString() != null ? "Token #" + order.getTokenString() : "Token #" + order.getTokenNumber());
            if (tvOrderPin != null) {
                tvOrderPin.setText("PIN: " + order.getPickupOtp());
            }
            tvCustomerName.setText(order.getUserName() != null ? order.getUserName() : "Customer #" + order.getUserId());
            tvOrderTotal.setText(String.format("₹%.2f", order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount()));

            if (order.getCreatedAt() != null) {
                CharSequence relative = DateUtils.getRelativeTimeSpanString(
                        order.getCreatedAt().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                );
                tvOrderTime.setText(relative);
            } else if (order.getOrderTimestamp() > 0) {
                CharSequence relative = DateUtils.getRelativeTimeSpanString(
                        order.getOrderTimestamp(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                );
                tvOrderTime.setText(relative);
            } else {
                tvOrderTime.setText("Just now");
            }

            // Build items text
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < order.getItems().size(); i++) {
                    OrderItem item = order.getItems().get(i);
                    sb.append("• ").append(item.getQuantity()).append("x ");
                    if (item.getItemName() != null) {
                        sb.append(item.getItemName());
                    } else {
                        sb.append("Item #").append(item.getItemId());
                    }
                    if (i < order.getItems().size() - 1) {
                        sb.append("\n");
                    }
                }
                tvOrderItems.setText(sb.toString());
            } else if (order.getItemsSummary() != null && !order.getItemsSummary().isEmpty()) {
                tvOrderItems.setText("• " + order.getItemsSummary().replace(", ", "\n• "));
            } else {
                tvOrderItems.setText("• Order details logged");
            }

            // Style status badge & next transition button
            String rawStatus = order.getOrderStatus();
            String status = rawStatus != null ? rawStatus.toUpperCase() : "PLACED";

            if ("PLACED".equals(status)) {
                tvOrderStatus.setText("Placed");
                tvOrderStatus.setTextColor(Color.parseColor("#C2410C"));
                tvOrderStatus.setBackgroundColor(Color.parseColor("#FFEDD5"));
                if (isKitchenInteractive) {
                    btnNextStatus.setVisibility(View.VISIBLE);
                    btnNextStatus.setText("Start Preparing");
                    btnNextStatus.setOnClickListener(v -> {
                        if (listener != null) listener.onStatusChangeRequested(order, "PREPARING");
                    });
                } else {
                    btnNextStatus.setVisibility(View.GONE);
                }
            } else if ("PREPARING".equals(status) || "ACCEPTED".equals(status)) {
                tvOrderStatus.setText("Preparing");
                tvOrderStatus.setTextColor(Color.parseColor("#1D4ED8"));
                tvOrderStatus.setBackgroundColor(Color.parseColor("#DBEAFE"));
                if (isKitchenInteractive) {
                    btnNextStatus.setVisibility(View.VISIBLE);
                    btnNextStatus.setText("Mark Ready for Pickup");
                    btnNextStatus.setOnClickListener(v -> {
                        if (listener != null) listener.onStatusChangeRequested(order, "READY_FOR_PICKUP");
                    });
                } else {
                    btnNextStatus.setVisibility(View.GONE);
                }
            } else if ("READY".equals(status) || "READY_FOR_PICKUP".equals(status)) {
                tvOrderStatus.setText("Ready for Pickup");
                tvOrderStatus.setTextColor(Color.parseColor("#047857"));
                tvOrderStatus.setBackgroundColor(Color.parseColor("#D1FAE5"));
                if (isKitchenInteractive) {
                    btnNextStatus.setVisibility(View.VISIBLE);
                    btnNextStatus.setText("Complete / Hand Over");
                    btnNextStatus.setOnClickListener(v -> {
                        if (listener != null) listener.onStatusChangeRequested(order, "COMPLETED");
                    });
                } else {
                    btnNextStatus.setVisibility(View.GONE);
                }
            } else if ("COMPLETED".equals(status)) {
                tvOrderStatus.setText("Completed");
                tvOrderStatus.setTextColor(Color.parseColor("#475569"));
                tvOrderStatus.setBackgroundColor(Color.parseColor("#E2E8F0"));
                btnNextStatus.setVisibility(View.GONE);
            } else {
                tvOrderStatus.setText(status);
                tvOrderStatus.setTextColor(Color.parseColor("#475569"));
                tvOrderStatus.setBackgroundColor(Color.parseColor("#E2E8F0"));
                btnNextStatus.setVisibility(View.GONE);
            }
        }
    }
}
