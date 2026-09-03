package com.example.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.adapter.KitchenOrderAdapter;
import com.example.db.CanteenDao;
import com.example.db.CollegeDao;
import com.example.db.DatabaseExecutor;
import com.example.db.OrderDao;
import com.example.model.College;
import com.example.model.Order;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class KitchenDashboardActivity extends AppCompatActivity {

    private MaterialToolbar toolbarKitchen;
    private TextView tvKitchenStaffName, tvCountPlaced, tvCountPreparing, tvCountReady, tvEmptyKitchen;
    private ChipGroup chipGroupKitchenFilter;
    private RecyclerView rvKitchenOrders;
    private MaterialButton btnRefreshKitchen, btnLogoutKitchen;
    private ProgressBar progressBarKitchen;

    private KitchenOrderAdapter kitchenOrderAdapter;
    private final OrderDao orderDao = new OrderDao();
    private final CollegeDao collegeDao = new CollegeDao();

    private int collegeId = 1;
    private String staffName = "Chef Staff";
    private String canteenId;
    private String canteenName;
    private String collegeName;
    private String currentStatusFilter = null; // null or "ALL" for all
    private final List<Order> loadedOrders = new ArrayList<>();
    private final CanteenDao canteenDao = new CanteenDao();

    private final Handler kitchenPollHandler = new Handler(Looper.getMainLooper());
    private Runnable kitchenPollRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kitchen_dashboard);

        extractIntentData();
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupFilterChips();
        loadKitchenOrders(true);
        setupAutoPolling();
    }

    private void setupAutoPolling() {
        kitchenPollRunnable = new Runnable() {
            @Override
            public void run() {
                loadKitchenOrders(false);
                kitchenPollHandler.postDelayed(this, 4000);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (kitchenPollRunnable != null) {
            kitchenPollHandler.removeCallbacks(kitchenPollRunnable);
            kitchenPollHandler.postDelayed(kitchenPollRunnable, 4000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (kitchenPollRunnable != null) {
            kitchenPollHandler.removeCallbacks(kitchenPollRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (kitchenPollRunnable != null) {
            kitchenPollHandler.removeCallbacks(kitchenPollRunnable);
        }
    }

    private void extractIntentData() {
        if (getIntent() != null) {
            collegeId = getIntent().getIntExtra("college_id", 1);
            String name = getIntent().getStringExtra("user_name");
            if (name != null) staffName = name;
            canteenId = getIntent().getStringExtra("canteen_id");
            canteenName = getIntent().getStringExtra("canteen_name");
            collegeName = getIntent().getStringExtra("college_name");
        }
    }

    private void initViews() {
        toolbarKitchen = findViewById(R.id.toolbarKitchen);
        tvKitchenStaffName = findViewById(R.id.tvKitchenStaffName);
        tvCountPlaced = findViewById(R.id.tvCountPlaced);
        tvCountPreparing = findViewById(R.id.tvCountPreparing);
        tvCountReady = findViewById(R.id.tvCountReady);
        tvEmptyKitchen = findViewById(R.id.tvEmptyKitchen);
        chipGroupKitchenFilter = findViewById(R.id.chipGroupKitchenFilter);
        rvKitchenOrders = findViewById(R.id.rvKitchenOrders);
        btnRefreshKitchen = findViewById(R.id.btnRefreshKitchen);
        btnLogoutKitchen = findViewById(R.id.btnLogoutKitchen);
        progressBarKitchen = findViewById(R.id.progressBarKitchen);

        String initialCanteenLabel = canteenName != null ? canteenName : "Assigned Canteen Kitchen";
        tvKitchenStaffName.setText(staffName + " • " + initialCanteenLabel);
        btnRefreshKitchen.setOnClickListener(v -> loadKitchenOrders());
        btnLogoutKitchen.setOnClickListener(v -> finish());
    }

    private void setupToolbar() {
        if (collegeName != null && canteenName != null) {
            toolbarKitchen.setSubtitle(collegeName + " • " + canteenName);
            tvKitchenStaffName.setText(staffName + " • " + canteenName + " Kitchen");
        }

        DatabaseExecutor.execute(() -> {
            College college = collegeDao.getCollegeById(collegeId);
            com.example.model.Canteen canteen = null;
            if (canteenId != null) {
                canteen = canteenDao.getCanteenById(canteenId);
            }
            if (canteen == null) {
                List<com.example.model.Canteen> list = canteenDao.getCanteensForCollege(collegeId);
                if (list != null && !list.isEmpty()) {
                    canteen = list.get(0);
                }
            }
            return new Object[]{college, canteen};
        }, new DatabaseExecutor.Callback<Object[]>() {
            @Override
            public void onSuccess(Object[] result) {
                College college = (College) result[0];
                com.example.model.Canteen canteen = (com.example.model.Canteen) result[1];
                String campus = collegeName != null ? collegeName : (college != null ? college.getName() : "Campus");
                String can = canteenName != null ? canteenName : (canteen != null ? canteen.getName() : "Kitchen");
                canteenName = can;
                collegeName = campus;
                toolbarKitchen.setSubtitle(campus + " • " + can);
                tvKitchenStaffName.setText(staffName + " • " + can + " Kitchen");
            }

            @Override
            public void onError(Exception e) {
                String campus = collegeName != null ? collegeName : "Campus";
                String can = canteenName != null ? canteenName : "Kitchen";
                toolbarKitchen.setSubtitle(campus + " • " + can);
            }
        });
    }

    private void setupRecyclerView() {
        kitchenOrderAdapter = new KitchenOrderAdapter(this::updateOrderStatusAsync, true);

        rvKitchenOrders.setLayoutManager(new LinearLayoutManager(this));
        rvKitchenOrders.setAdapter(kitchenOrderAdapter);
    }

    private void setupFilterChips() {
        chipGroupKitchenFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chipKitchenAll) {
                currentStatusFilter = null;
            } else if (checkedId == R.id.chipFilterPlaced) {
                currentStatusFilter = "Placed";
            } else if (checkedId == R.id.chipFilterPreparing) {
                currentStatusFilter = "Preparing";
            } else if (checkedId == R.id.chipFilterReady) {
                currentStatusFilter = "Ready";
            } else if (checkedId == R.id.chipFilterCompleted) {
                currentStatusFilter = "Completed";
            }
            applyCurrentFilter();
        });
    }

    private void loadKitchenOrders(boolean showLoading) {
        if (showLoading) {
            progressBarKitchen.setVisibility(View.VISIBLE);
        }
        DatabaseExecutor.execute(() -> orderDao.getOrdersByCanteen(canteenId, collegeId), new DatabaseExecutor.Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> orders) {
                if (showLoading) {
                    progressBarKitchen.setVisibility(View.GONE);
                }
                loadedOrders.clear();
                if (orders != null) {
                    loadedOrders.addAll(orders);
                }
                updateMetrics();
                applyCurrentFilter();
            }

            @Override
            public void onError(Exception e) {
                if (showLoading) {
                    progressBarKitchen.setVisibility(View.GONE);
                    Toast.makeText(KitchenDashboardActivity.this, "Error fetching orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadKitchenOrders() {
        loadKitchenOrders(true);
    }

    private void updateMetrics() {
        int placed = 0, preparing = 0, ready = 0;
        for (Order o : loadedOrders) {
            String s = o.getOrderStatus();
            if (s != null) {
                String u = s.toUpperCase();
                if (u.equals("PLACED")) placed++;
                else if (u.equals("PREPARING") || u.equals("ACCEPTED")) preparing++;
                else if (u.equals("READY") || u.equals("READY_FOR_PICKUP")) ready++;
            }
        }
        tvCountPlaced.setText(String.valueOf(placed));
        tvCountPreparing.setText(String.valueOf(preparing));
        tvCountReady.setText(String.valueOf(ready));
    }

    private void applyCurrentFilter() {
        List<Order> filtered = new ArrayList<>();
        for (Order o : loadedOrders) {
            if (currentStatusFilter == null || currentStatusFilter.equalsIgnoreCase("ALL")) {
                filtered.add(o);
            } else {
                String s = o.getOrderStatus();
                if (s != null) {
                    String u = s.toUpperCase();
                    String f = currentStatusFilter.toUpperCase();
                    if (f.equals("READY") && (u.equals("READY") || u.equals("READY_FOR_PICKUP"))) {
                        filtered.add(o);
                    } else if (f.equals("PREPARING") && (u.equals("PREPARING") || u.equals("ACCEPTED"))) {
                        filtered.add(o);
                    } else if (u.equals(f)) {
                        filtered.add(o);
                    }
                }
            }
        }

        kitchenOrderAdapter.setOrders(filtered);
        if (filtered.isEmpty()) {
            tvEmptyKitchen.setVisibility(View.VISIBLE);
            String targetCanteen = canteenName != null ? canteenName : "this canteen";
            tvEmptyKitchen.setText("No active orders for " + targetCanteen + " right now.");
        } else {
            tvEmptyKitchen.setVisibility(View.GONE);
        }
    }

    /**
     * Executes off-thread JDBC update via DatabaseExecutor.java to modify order_status in Cloud SQL,
     * and smoothly updates UI without reloading the whole screen.
     */
    private void updateOrderStatusAsync(Order order, String newStatus) {
        DatabaseExecutor.execute(() -> orderDao.updateOrderStatus(order.getIdString(), order.getOrderNumber(), order.getTokenString(), newStatus), new DatabaseExecutor.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                order.setOrderStatus(newStatus);
                kitchenOrderAdapter.notifyDataSetChanged();
                updateMetrics();
                Toast.makeText(KitchenDashboardActivity.this, "Token #" + order.getTokenString() + " updated to " + newStatus, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(KitchenDashboardActivity.this, "Status update error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
