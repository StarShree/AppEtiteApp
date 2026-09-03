package com.example.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.adapter.AdminMenuAdapter;
import com.example.adapter.KitchenOrderAdapter;
import com.example.db.CanteenDao;
import com.example.db.CollegeDao;
import com.example.db.DatabaseExecutor;
import com.example.db.MenuItemDao;
import com.example.db.OrderDao;
import com.example.model.College;
import com.example.model.MenuItem;
import com.example.model.Order;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CollegeAdminDashboardActivity extends AppCompatActivity {

    private MaterialToolbar toolbarAdmin;
    private TextView tvCollegeAdminName, tvAdminTotalItems, tvAdminTotalOrders, tvAdminOrdersEmpty;
    private MaterialButton btnAddNewMenuItem, btnLogoutCollegeAdmin;
    private MaterialButton btnTabMenuCatalog, btnTabCollegeOrders, btnRefreshCollegeOrders;
    private MaterialCardView cardAdminMenuItemsStat, cardAdminOrdersStat;
    private View layoutMenuCatalogSection, layoutOrdersSection;
    private RecyclerView rvAdminMenu, rvAdminOrders;
    private ProgressBar progressBarAdmin;

    private AdminMenuAdapter adminMenuAdapter;
    private KitchenOrderAdapter collegeOrdersAdapter;
    private final MenuItemDao menuItemDao = new MenuItemDao();
    private final OrderDao orderDao = new OrderDao();
    private final CollegeDao collegeDao = new CollegeDao();
    private final CanteenDao canteenDao = new CanteenDao();

    private int collegeId = 1;
    private String adminName = "Prof. Robert Davis";
    private String collegeName = null;
    private String canteenId;
    private final List<MenuItem> menuItemsList = new ArrayList<>();
    private final List<Order> collegeOrdersList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_college_admin_dashboard);

        extractIntentData();
        initViews();
        setupToolbar();
        setupRecyclerView();
        loadDashboardData();
    }

    private void extractIntentData() {
        if (getIntent() != null) {
            collegeId = getIntent().getIntExtra("college_id", 1);
            String name = getIntent().getStringExtra("user_name");
            if (name != null) adminName = name;
            canteenId = getIntent().getStringExtra("canteen_id");
            String colName = getIntent().getStringExtra("college_name");
            if (colName != null && !colName.isEmpty()) collegeName = colName;
        }
    }

    private void initViews() {
        toolbarAdmin = findViewById(R.id.toolbarAdmin);
        tvCollegeAdminName = findViewById(R.id.tvCollegeAdminName);
        tvAdminTotalItems = findViewById(R.id.tvAdminTotalItems);
        tvAdminTotalOrders = findViewById(R.id.tvAdminTotalOrders);
        tvAdminOrdersEmpty = findViewById(R.id.tvAdminOrdersEmpty);
        btnAddNewMenuItem = findViewById(R.id.btnAddNewMenuItem);
        btnLogoutCollegeAdmin = findViewById(R.id.btnLogoutCollegeAdmin);
        btnTabMenuCatalog = findViewById(R.id.btnTabMenuCatalog);
        btnTabCollegeOrders = findViewById(R.id.btnTabCollegeOrders);
        btnRefreshCollegeOrders = findViewById(R.id.btnRefreshCollegeOrders);
        cardAdminMenuItemsStat = findViewById(R.id.cardAdminMenuItemsStat);
        cardAdminOrdersStat = findViewById(R.id.cardAdminOrdersStat);
        layoutMenuCatalogSection = findViewById(R.id.layoutMenuCatalogSection);
        layoutOrdersSection = findViewById(R.id.layoutOrdersSection);
        rvAdminMenu = findViewById(R.id.rvAdminMenu);
        rvAdminOrders = findViewById(R.id.rvAdminOrders);
        progressBarAdmin = findViewById(R.id.progressBarAdmin);

        String titleAffix = collegeName != null ? " • " + collegeName : "";
        tvCollegeAdminName.setText(adminName + titleAffix + " (Admin Console)");
        btnAddNewMenuItem.setOnClickListener(v -> showAddMenuItemDialog());
        btnLogoutCollegeAdmin.setOnClickListener(v -> finish());

        cardAdminMenuItemsStat.setOnClickListener(v -> switchTab(true));
        cardAdminOrdersStat.setOnClickListener(v -> switchTab(false));
        btnTabMenuCatalog.setOnClickListener(v -> switchTab(true));
        btnTabCollegeOrders.setOnClickListener(v -> switchTab(false));
        btnRefreshCollegeOrders.setOnClickListener(v -> loadCollegeOrders());
    }

    private void setupToolbar() {
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
                String can = canteen != null ? canteen.getName() : "Canteens";
                toolbarAdmin.setSubtitle(campus + " • " + can);
            }

            @Override
            public void onError(Exception e) {
                toolbarAdmin.setSubtitle("College Admin");
            }
        });
    }

    private void setupRecyclerView() {
        adminMenuAdapter = new AdminMenuAdapter(new AdminMenuAdapter.MenuAdminListener() {
            @Override
            public void onAvailabilityToggled(MenuItem item, boolean isAvailable) {
                DatabaseExecutor.execute(
                        () -> menuItemDao.updateMenuItemAvailability(item.getItemId(), isAvailable),
                        new DatabaseExecutor.Callback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean success) {
                                item.setAvailable(isAvailable);
                                Toast.makeText(CollegeAdminDashboardActivity.this,
                                        item.getName() + " availability: " + (isAvailable ? "Available" : "Sold Out"),
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(CollegeAdminDashboardActivity.this, "Error updating availability", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }

            @Override
            public void onItemDeleteRequested(MenuItem item) {
                new MaterialAlertDialogBuilder(CollegeAdminDashboardActivity.this)
                        .setTitle("Delete Menu Item")
                        .setMessage("Are you sure you want to remove '" + item.getName() + "' from the canteen catalog?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            DatabaseExecutor.execute(() -> menuItemDao.deleteMenuItem(item.getItemId()), new DatabaseExecutor.Callback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean success) {
                                    loadDashboardData();
                                    Toast.makeText(CollegeAdminDashboardActivity.this, "Item deleted", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(CollegeAdminDashboardActivity.this, "Error deleting item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        rvAdminMenu.setLayoutManager(new LinearLayoutManager(this));
        rvAdminMenu.setAdapter(adminMenuAdapter);

        collegeOrdersAdapter = new KitchenOrderAdapter(this::updateOrderStatusAsync, true);
        rvAdminOrders.setLayoutManager(new LinearLayoutManager(this));
        rvAdminOrders.setAdapter(collegeOrdersAdapter);
    }

    private void switchTab(boolean showMenu) {
        if (showMenu) {
            layoutMenuCatalogSection.setVisibility(View.VISIBLE);
            layoutOrdersSection.setVisibility(View.GONE);
        } else {
            layoutMenuCatalogSection.setVisibility(View.GONE);
            layoutOrdersSection.setVisibility(View.VISIBLE);
            loadCollegeOrders();
        }
    }

    private void updateOrderStatusAsync(Order order, String newStatus) {
        DatabaseExecutor.execute(() -> orderDao.updateOrderStatus(order.getIdString(), order.getOrderNumber(), order.getTokenString(), newStatus), new DatabaseExecutor.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Toast.makeText(CollegeAdminDashboardActivity.this, "Order " + (order.getTokenString() != null ? "#" + order.getTokenString() : "") + " updated to " + newStatus, Toast.LENGTH_SHORT).show();
                loadCollegeOrders();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(CollegeAdminDashboardActivity.this, "Error updating status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCollegeOrders() {
        DatabaseExecutor.execute(() -> orderDao.getOrdersByCollege(collegeId), new DatabaseExecutor.Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> orders) {
                collegeOrdersList.clear();
                if (orders != null) {
                    collegeOrdersList.addAll(orders);
                }
                tvAdminTotalOrders.setText(String.valueOf(collegeOrdersList.size()));
                collegeOrdersAdapter.setOrders(collegeOrdersList);
                if (tvAdminOrdersEmpty != null) {
                    tvAdminOrdersEmpty.setVisibility(collegeOrdersList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onError(Exception e) {
                // ignore
            }
        });
    }

    private void loadDashboardData() {
        progressBarAdmin.setVisibility(View.VISIBLE);

        // Load items
        DatabaseExecutor.execute(() -> menuItemDao.getMenuItemsByCollege(collegeId, "All"), new DatabaseExecutor.Callback<List<MenuItem>>() {
            @Override
            public void onSuccess(List<MenuItem> items) {
                progressBarAdmin.setVisibility(View.GONE);
                menuItemsList.clear();
                if (items != null) {
                    menuItemsList.addAll(items);
                }
                adminMenuAdapter.setItems(menuItemsList);
                tvAdminTotalItems.setText(String.valueOf(menuItemsList.size()));
            }

            @Override
            public void onError(Exception e) {
                progressBarAdmin.setVisibility(View.GONE);
                Toast.makeText(CollegeAdminDashboardActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Load college orders
        loadCollegeOrders();
    }

    private void showAddMenuItemDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_menu_item, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        TextInputEditText etName = dialogView.findViewById(R.id.etDialogItemName);
        TextInputEditText etDesc = dialogView.findViewById(R.id.etDialogItemDesc);
        TextInputEditText etPrice = dialogView.findViewById(R.id.etDialogItemPrice);
        TextInputEditText etImage = dialogView.findViewById(R.id.etDialogItemImage);
        Spinner spinnerCat = dialogView.findViewById(R.id.spinnerDialogCategory);

        String[] categories = {"Breakfast", "Lunch", "Beverages", "Snacks"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCat.setAdapter(catAdapter);

        dialogView.findViewById(R.id.btnCancelAddDialog).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSaveMenuItemDialog).setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
            String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
            String img = etImage.getText() != null ? etImage.getText().toString().trim() : "";
            String cat = categories[spinnerCat.getSelectedItemPosition()];

            if (name.isEmpty()) {
                etName.setError("Name is required");
                return;
            }
            if (priceStr.isEmpty()) {
                etPrice.setError("Price is required");
                return;
            }

            BigDecimal price;
            try {
                price = new BigDecimal(priceStr);
            } catch (Exception ex) {
                etPrice.setError("Invalid price format");
                return;
            }

            MenuItem newItem = new MenuItem(0, collegeId, name, desc, price, cat, img, true);
            DatabaseExecutor.execute(() -> menuItemDao.insertMenuItem(newItem), new DatabaseExecutor.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean success) {
                    dialog.dismiss();
                    loadDashboardData();
                    Toast.makeText(CollegeAdminDashboardActivity.this, "Item added to catalog!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(CollegeAdminDashboardActivity.this, "Error adding item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }
}
