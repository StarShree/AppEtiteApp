package com.example.activity;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.adapter.KitchenOrderAdapter;
import com.example.db.CollegeDao;
import com.example.db.DatabaseExecutor;
import com.example.db.OrderDao;
import com.example.db.UserDao;
import com.example.model.College;
import com.example.model.Order;
import com.example.model.User;
import com.google.android.material.button.MaterialButton;

import java.math.BigDecimal;
import java.util.List;

public class SuperAdminDashboardActivity extends AppCompatActivity {

    private TextView tvSuperAdminRole, tvSuperAdminDbStatus, tvStatColleges, tvStatUsers, tvStatOrders, tvStatRevenue;
    private MaterialButton btnLogoutSuperAdmin;
    private RecyclerView rvSuperAdminOrders;

    private KitchenOrderAdapter ordersFeedAdapter;
    private final OrderDao orderDao = new OrderDao();
    private final UserDao userDao = new UserDao();
    private final CollegeDao collegeDao = new CollegeDao();

    private String role = "SUPER_ADMIN";
    private String adminName = "Chief System Administrator";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_super_admin_dashboard);

        extractIntentData();
        initViews();
        setupRecyclerView();
        loadGlobalMetrics();
    }

    private void extractIntentData() {
        if (getIntent() != null) {
            String r = getIntent().getStringExtra("role");
            if (r != null) role = r;
            String n = getIntent().getStringExtra("user_name");
            if (n != null) adminName = n;
        }
    }

    private void initViews() {
        tvSuperAdminRole = findViewById(R.id.tvSuperAdminRole);
        tvSuperAdminDbStatus = findViewById(R.id.tvSuperAdminDbStatus);
        tvStatColleges = findViewById(R.id.tvStatColleges);
        tvStatUsers = findViewById(R.id.tvStatUsers);
        tvStatOrders = findViewById(R.id.tvStatOrders);
        tvStatRevenue = findViewById(R.id.tvStatRevenue);
        btnLogoutSuperAdmin = findViewById(R.id.btnLogoutSuperAdmin);
        rvSuperAdminOrders = findViewById(R.id.rvSuperAdminOrders);

        tvSuperAdminRole.setText(adminName + " • " + role);
        btnLogoutSuperAdmin.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        ordersFeedAdapter = new KitchenOrderAdapter(this::updateOrderStatusAsync, true);
        rvSuperAdminOrders.setLayoutManager(new LinearLayoutManager(this));
        rvSuperAdminOrders.setAdapter(ordersFeedAdapter);
    }

    private void updateOrderStatusAsync(Order order, String newStatus) {
        DatabaseExecutor.execute(() -> orderDao.updateOrderStatus(order.getIdString(), order.getOrderNumber(), order.getTokenString(), newStatus), new DatabaseExecutor.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Toast.makeText(SuperAdminDashboardActivity.this, "Order " + (order.getTokenString() != null ? "#" + order.getTokenString() : "") + " updated to " + newStatus, Toast.LENGTH_SHORT).show();
                loadGlobalMetrics();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(SuperAdminDashboardActivity.this, "Error updating status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGlobalMetrics() {
        // Colleges count
        DatabaseExecutor.execute(collegeDao::getAllColleges, new DatabaseExecutor.Callback<List<College>>() {
            @Override
            public void onSuccess(List<College> colleges) {
                if (colleges != null) {
                    tvStatColleges.setText(String.valueOf(colleges.size()));
                }
            }

            @Override
            public void onError(Exception e) {
                // Ignore
            }
        });

        // Users count
        DatabaseExecutor.execute(userDao::getAllUsers, new DatabaseExecutor.Callback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users != null) {
                    tvStatUsers.setText(String.valueOf(users.size()));
                }
            }

            @Override
            public void onError(Exception e) {
                // Ignore
            }
        });

        // All Orders and Gross Revenue
        DatabaseExecutor.execute(orderDao::getAllOrders, new DatabaseExecutor.Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> orders) {
                if (orders != null) {
                    tvStatOrders.setText(String.valueOf(orders.size()));
                    BigDecimal totalRev = BigDecimal.ZERO;
                    for (Order o : orders) {
                        if (o.getTotalAmount() != null) {
                            totalRev = totalRev.add(o.getTotalAmount());
                        }
                    }
                    tvStatRevenue.setText(String.format("₹%.2f", totalRev));
                    ordersFeedAdapter.setOrders(orders);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(SuperAdminDashboardActivity.this, "Error fetching global feed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
