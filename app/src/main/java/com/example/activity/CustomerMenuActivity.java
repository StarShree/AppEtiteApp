package com.example.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.adapter.CartAdapter;
import com.example.adapter.CustomerMenuAdapter;
import com.example.adapter.KitchenOrderAdapter;
import com.example.db.CanteenDao;
import com.example.db.CollegeDao;
import com.example.db.DatabaseExecutor;
import com.example.db.MenuItemDao;
import com.example.db.OrderDao;
import com.example.db.UserDao;
import com.example.model.Canteen;
import com.example.model.College;
import com.example.model.MenuItem;
import com.example.model.Order;
import com.example.model.OrderItem;
import com.example.util.ThemeHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMenuActivity extends AppCompatActivity {

    private int userId;
    private String userIdString;
    private int collegeId = 3; // Default to Stanford (col_3)
    private String userName = "Alex Rivera";
    private String canteenId = "canteen_3_1";
    private String canteenName = "Byte Bites Cyber Cafe";
    private BigDecimal walletBalance = new BigDecimal("350.00");

    // Live Sync Engine with Kitchen Orders Table
    private final Handler liveSyncHandler = new Handler(Looper.getMainLooper());
    private Runnable liveSyncRunnable;
    private final Map<String, String> lastKnownStatusMap = new HashMap<>();
    private View layoutLiveSyncStatus;
    private TextView tvLiveSyncStatus;
    private View btnLiveSyncRefresh;
    private BottomSheetDialog activeTrackerSheet;
    private View activeTrackerRoot;
    private Order activeTrackerOrder;

    // Top Bar & Navigation
    private MaterialCardView btnToggleTheme;
    private ImageView ivThemeIconCustomer;
    private View btnCartHeader;
    private TextView tvCartBadge;
    private MaterialCardView btnLogoutCustomer;
    private MaterialCardView btnChangeCollegeCanteen;
    private TextView tvActiveCollegeBar, tvActiveCanteenBar;

    // Tabs
    private View layoutMenuTab, layoutTokensTab;
    private View navTabMenu, navTabTokens;
    private TextView tvNavMenuIcon, tvNavMenuLabel, tvNavTokensIcon, tvNavTokensLabel;

    // Hero Banner
    private TextView tvHeroWelcome, tvHeroSubtitle;

    // Live Order Status Section
    private View layoutLiveOrderSection;
    private TextView tvLiveTokenPill, tvLiveOrderCanteen, tvLiveOrderStatusBadge, tvLiveOrderStepper, tvLiveOrderItemsSummary, tvLiveOrderPrice;
    private MaterialButton btnSimulateProgress;
    private Order currentLiveOrder;

    // Wallet Section
    private TextView tvWalletBalanceDisplay;
    private MaterialButton btnTopUpWallet, btnQuickAdd10, btnQuickAdd25, btnQuickAdd50;
    private LinearLayout layoutLedgerItems;

    // Search & Filters
    private EditText etSearchDishes;
    private MaterialButton btnFilterAllCanteens, btnFilterCanteen1, btnFilterCanteen2;
    private ChipGroup chipGroupCategories;
    private TextView tvMenuHeaderCount;
    private ProgressBar progressBarMenu;
    private RecyclerView rvCustomerMenu;

    // Floating Cart Bar
    private MaterialCardView cardCartBar;
    private TextView tvCartSummary, tvNextTokenPreview;
    private MaterialButton btnCheckout;

    // Tokens Tab
    private View btnSubTabLiveTokens, btnSubTabHistory;
    private TextView tvSubTabLiveTokensTitle, tvSubTabHistoryTitle;
    private View indicatorSubTabLiveTokens, indicatorSubTabHistory;
    private LinearLayout layoutTokensContainer;
    private boolean isShowingHistoryInTokens = false;

    // Data & Adapters
    private CustomerMenuAdapter menuAdapter;
    private final MenuItemDao menuItemDao = new MenuItemDao();
    private final CollegeDao collegeDao = new CollegeDao();
    private final CanteenDao canteenDao = new CanteenDao();
    private final OrderDao orderDao = new OrderDao();
    private final UserDao userDao = new UserDao();

    private List<MenuItem> allMenuItems = new ArrayList<>();
    private List<MenuItem> displayedMenuItems = new ArrayList<>();
    private BigDecimal currentTotal = BigDecimal.ZERO;

    private String activeCategoryFilter = "All";
    private String activeSearchQuery = "";
    private int activeStallFilterIndex = 0; // 0 = All, 1 = Canteen1, 2 = Canteen2

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_menu);

        readIntentData();
        initViews();
        setupThemeToggle();
        setupCollegeAndCanteenBar();
        setupBottomNavigation();
        setupLiveOrderSection();
        setupWalletSection();
        setupSearchAndFilters();
        setupMenuRecyclerView();

        loadActiveCollegeInfo();
        loadMenu();
        refreshLiveOrders();
        setupLiveSync();
    }

    private void setupLiveSync() {
        liveSyncRunnable = new Runnable() {
            @Override
            public void run() {
                syncOrdersWithKitchen(false);
                liveSyncHandler.postDelayed(this, 3500);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (liveSyncRunnable != null) {
            liveSyncHandler.removeCallbacks(liveSyncRunnable);
            liveSyncHandler.postDelayed(liveSyncRunnable, 500);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (liveSyncRunnable != null) {
            liveSyncHandler.removeCallbacks(liveSyncRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (liveSyncRunnable != null) {
            liveSyncHandler.removeCallbacks(liveSyncRunnable);
        }
    }

    private void readIntentData() {
        userId = getIntent().getIntExtra("USER_ID", getIntent().getIntExtra("user_id", 1));
        userIdString = getIntent().getStringExtra("USER_ID_STR");
        if (userIdString == null) userIdString = getIntent().getStringExtra("user_id_str");
        if (userIdString == null || userIdString.trim().isEmpty()) {
            userIdString = "user_" + userId;
        }

        collegeId = getIntent().getIntExtra("COLLEGE_ID", getIntent().getIntExtra("college_id", 3));
        String name = getIntent().getStringExtra("USER_NAME");
        if (name == null) name = getIntent().getStringExtra("user_name");
        if (name != null && !name.trim().isEmpty()) {
            userName = name.trim();
        }

        String cId = getIntent().getStringExtra("CANTEEN_ID");
        if (cId == null) cId = getIntent().getStringExtra("canteen_id");
        if (cId != null && !cId.trim().isEmpty()) {
            canteenId = cId.trim();
        }

        String cName = getIntent().getStringExtra("CANTEEN_NAME");
        if (cName == null) cName = getIntent().getStringExtra("canteen_name");
        if (cName != null && !cName.trim().isEmpty()) {
            canteenName = cName.trim();
        }
    }

    private void initViews() {
        btnToggleTheme = findViewById(R.id.btnToggleTheme);
        ivThemeIconCustomer = findViewById(R.id.ivThemeIconCustomer);
        btnCartHeader = findViewById(R.id.btnCartHeader);
        tvCartBadge = findViewById(R.id.tvCartBadge);
        btnChangeCollegeCanteen = findViewById(R.id.btnChangeCollegeCanteen);
        tvActiveCollegeBar = findViewById(R.id.tvActiveCollegeBar);
        tvActiveCanteenBar = findViewById(R.id.tvActiveCanteenBar);

        layoutMenuTab = findViewById(R.id.layoutMenuTab);
        layoutTokensTab = findViewById(R.id.layoutTokensTab);
        navTabMenu = findViewById(R.id.navTabMenu);
        navTabTokens = findViewById(R.id.navTabTokens);
        tvNavMenuIcon = findViewById(R.id.tvNavMenuIcon);
        tvNavMenuLabel = findViewById(R.id.tvNavMenuLabel);
        tvNavTokensIcon = findViewById(R.id.tvNavTokensIcon);
        tvNavTokensLabel = findViewById(R.id.tvNavTokensLabel);

        tvHeroWelcome = findViewById(R.id.tvHeroWelcome);
        tvHeroSubtitle = findViewById(R.id.tvHeroSubtitle);

        layoutLiveOrderSection = findViewById(R.id.layoutLiveOrderSection);
        tvLiveTokenPill = findViewById(R.id.tvLiveTokenPill);
        tvLiveOrderCanteen = findViewById(R.id.tvLiveOrderCanteen);
        tvLiveOrderStatusBadge = findViewById(R.id.tvLiveOrderStatusBadge);
        tvLiveOrderStepper = findViewById(R.id.tvLiveOrderStepper);
        tvLiveOrderItemsSummary = findViewById(R.id.tvLiveOrderItemsSummary);
        tvLiveOrderPrice = findViewById(R.id.tvLiveOrderPrice);
        btnSimulateProgress = findViewById(R.id.btnSimulateProgress);

        tvWalletBalanceDisplay = findViewById(R.id.tvWalletBalanceDisplay);
        btnTopUpWallet = findViewById(R.id.btnTopUpWallet);
        btnQuickAdd10 = findViewById(R.id.btnQuickAdd10);
        btnQuickAdd25 = findViewById(R.id.btnQuickAdd25);
        btnQuickAdd50 = findViewById(R.id.btnQuickAdd50);
        layoutLedgerItems = findViewById(R.id.layoutLedgerItems);

        etSearchDishes = findViewById(R.id.etSearchDishes);
        btnFilterAllCanteens = findViewById(R.id.btnFilterAllCanteens);
        btnFilterCanteen1 = findViewById(R.id.btnFilterCanteen1);
        btnFilterCanteen2 = findViewById(R.id.btnFilterCanteen2);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        tvMenuHeaderCount = findViewById(R.id.tvMenuHeaderCount);
        progressBarMenu = findViewById(R.id.progressBarMenu);
        rvCustomerMenu = findViewById(R.id.rvCustomerMenu);

        cardCartBar = findViewById(R.id.cardCartBar);
        tvCartSummary = findViewById(R.id.tvCartSummary);
        tvNextTokenPreview = findViewById(R.id.tvNextTokenPreview);
        btnCheckout = findViewById(R.id.btnCheckout);

        btnSubTabLiveTokens = findViewById(R.id.btnSubTabLiveTokens);
        btnSubTabHistory = findViewById(R.id.btnSubTabHistory);
        tvSubTabLiveTokensTitle = findViewById(R.id.tvSubTabLiveTokensTitle);
        tvSubTabHistoryTitle = findViewById(R.id.tvSubTabHistoryTitle);
        indicatorSubTabLiveTokens = findViewById(R.id.indicatorSubTabLiveTokens);
        indicatorSubTabHistory = findViewById(R.id.indicatorSubTabHistory);
        layoutTokensContainer = findViewById(R.id.layoutTokensContainer);

        layoutLiveSyncStatus = findViewById(R.id.layoutLiveSyncStatus);
        tvLiveSyncStatus = findViewById(R.id.tvLiveSyncStatus);
        btnLiveSyncRefresh = findViewById(R.id.btnLiveSyncRefresh);
        if (btnLiveSyncRefresh != null) {
            btnLiveSyncRefresh.setOnClickListener(v -> syncOrdersWithKitchen(true));
        }
        if (layoutLiveSyncStatus != null) {
            layoutLiveSyncStatus.setOnClickListener(v -> syncOrdersWithKitchen(true));
        }

        tvHeroWelcome.setText("Welcome, " + userName + "!");

        btnLogoutCustomer = findViewById(R.id.btnLogoutCustomer);
        if (btnLogoutCustomer != null) {
            btnLogoutCustomer.setOnClickListener(v -> confirmLogout());
        }

        btnCartHeader.setOnClickListener(v -> showCartBottomSheet());
        cardCartBar.setOnClickListener(v -> showCartBottomSheet());
        btnCheckout.setOnClickListener(v -> showCartBottomSheet());
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of your AppEtite account?")
                .setIcon(R.drawable.ic_logout)
                .setPositiveButton("Log Out", (dialog, which) -> {
                    Toast.makeText(CustomerMenuActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CustomerMenuActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupThemeToggle() {
        if (btnToggleTheme != null && ivThemeIconCustomer != null) {
            boolean isDark = ThemeHelper.isDarkMode(this);
            ivThemeIconCustomer.setImageResource(isDark ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
            btnToggleTheme.setOnClickListener(v -> {
                ThemeHelper.toggleTheme(CustomerMenuActivity.this);
                recreate();
            });
        }
    }

    /**
     * Outstanding requirement: Option to change college and their respective canteen in the dashboard!
     */
    private void setupCollegeAndCanteenBar() {
        btnChangeCollegeCanteen.setOnClickListener(v -> showSwitchCollegeCanteenBottomSheet());
    }

    private void showSwitchCollegeCanteenBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View root = LayoutInflater.from(this).inflate(R.layout.dialog_switch_campus_canteen, null);
        sheet.setContentView(root);

        root.findViewById(R.id.btnCloseSwitchModal).setOnClickListener(v -> sheet.dismiss());

        RadioGroup rgColleges = root.findViewById(R.id.rgColleges);
        RadioGroup rgCanteens = root.findViewById(R.id.rgCanteens);
        RadioButton rbCollege1 = root.findViewById(R.id.rbCollege1);
        RadioButton rbCollege2 = root.findViewById(R.id.rbCollege2);
        RadioButton rbCollege3 = root.findViewById(R.id.rbCollege3);
        RadioButton rbCanteen1 = root.findViewById(R.id.rbCanteen1);
        RadioButton rbCanteen2 = root.findViewById(R.id.rbCanteen2);
        MaterialButton btnConfirmSwitch = root.findViewById(R.id.btnConfirmSwitch);

        // Preselect active college
        if (collegeId == 3) {
            rbCollege1.setChecked(true);
            updateCanteensForSelectedCollege(3, rbCanteen1, rbCanteen2);
        } else if (collegeId == 1) {
            rbCollege2.setChecked(true);
            updateCanteensForSelectedCollege(1, rbCanteen1, rbCanteen2);
        } else {
            rbCollege3.setChecked(true);
            updateCanteensForSelectedCollege(2, rbCanteen1, rbCanteen2);
        }

        // Listener when user clicks another college radio button
        rgColleges.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCollege1) {
                updateCanteensForSelectedCollege(3, rbCanteen1, rbCanteen2);
            } else if (checkedId == R.id.rbCollege2) {
                updateCanteensForSelectedCollege(1, rbCanteen1, rbCanteen2);
            } else if (checkedId == R.id.rbCollege3) {
                updateCanteensForSelectedCollege(2, rbCanteen1, rbCanteen2);
            }
        });

        btnConfirmSwitch.setOnClickListener(v -> {
            int selectedColId = 3;
            String chosenColName = "Stanford University Campus";
            if (rbCollege2.isChecked()) {
                selectedColId = 1;
                chosenColName = "Imperial College of Engineering";
            } else if (rbCollege3.isChecked()) {
                selectedColId = 2;
                chosenColName = "St. Jude Institute of Technology";
            }

            String chosenCanteenId = "canteen_" + selectedColId + "_1";
            String chosenCanteenName = rbCanteen1.getText().toString().split("\n")[0];
            if (rbCanteen2.isChecked()) {
                chosenCanteenId = "canteen_" + selectedColId + "_2";
                chosenCanteenName = rbCanteen2.getText().toString().split("\n")[0];
            }

            // Apply new college & canteen
            collegeId = selectedColId;
            canteenId = chosenCanteenId;
            canteenName = chosenCanteenName;

            updateActiveBarTitles(chosenColName, chosenCanteenName);

            // Persist to Cloud SQL / Local Store in background
            final int fCol = collegeId;
            final String fCanteenId = canteenId;
            DatabaseExecutor.execute(() -> userDao.updateUserCollegeAndCanteen(userId, null, fCol, "col_" + fCol, fCanteenId), null);

            sheet.dismiss();
            Toast.makeText(CustomerMenuActivity.this, "Switched to " + chosenColName + " • " + chosenCanteenName, Toast.LENGTH_SHORT).show();

            // Refresh menu & stalls
            loadMenu();
            refreshLiveOrders();
        });

        sheet.show();
    }

    private void updateCanteensForSelectedCollege(int colId, RadioButton rb1, RadioButton rb2) {
        if (colId == 3) {
            rb1.setText("Byte Bites Cyber Cafe\nEngineering Block, Level 1 (07:30 AM - 09:30 PM)");
            rb2.setText("Cardinal Quad Food Court\nStudent Union Plaza (08:00 AM - 10:00 PM)");
        } else if (colId == 1) {
            rb1.setText("Engineering Main Canteen\nBlock B, Central Dining Hall (08:00 AM - 08:00 PM)");
            rb2.setText("Crown Snack Bar\nInnovation Park, Ground Floor (09:00 AM - 10:00 PM)");
        } else {
            rb1.setText("St. Jude Central Hall Canteen\nMain Quadrangle (07:30 AM - 09:00 PM)");
            rb2.setText("Pavilion Deli & Juice Bar\nSports Complex Level 1 (08:30 AM - 09:30 PM)");
        }
        rb1.setChecked(true);
    }

    private void updateActiveBarTitles(String colName, String cantName) {
        tvActiveCollegeBar.setText(colName);
        tvActiveCanteenBar.setText("🏪 " + cantName);
        tvHeroSubtitle.setText("Campus: " + colName + " • Connected to MySQL");

        // Update Stall filter pills
        if (collegeId == 3) {
            btnFilterCanteen1.setText("🏪 Byte Bites ★ 4.9 • 10m");
            btnFilterCanteen2.setText("🏪 Cardinal Quad ★ 4.7 • 15m");
        } else if (collegeId == 1) {
            btnFilterCanteen1.setText("🏪 Main Canteen ★ 4.8 • 8m");
            btnFilterCanteen2.setText("🏪 Crown Snack ★ 4.6 • 12m");
        } else {
            btnFilterCanteen1.setText("🏪 Central Hall ★ 4.8 • 10m");
            btnFilterCanteen2.setText("🏪 Pavilion Deli ★ 4.7 • 14m");
        }
    }

    private void loadActiveCollegeInfo() {
        DatabaseExecutor.execute(() -> {
            College college = collegeDao.getCollegeById(collegeId);
            List<Canteen> list = canteenDao.getCanteensForCollege("col_" + collegeId);
            return new Object[]{college, list};
        }, new DatabaseExecutor.Callback<Object[]>() {
            @Override
            public void onSuccess(Object[] result) {
                College col = (College) result[0];
                @SuppressWarnings("unchecked")
                List<Canteen> canteens = (List<Canteen>) result[1];
                String cName = col != null ? col.getName() : "Stanford University Campus";

                if (canteens != null && !canteens.isEmpty()) {
                    boolean found = false;
                    for (Canteen c : canteens) {
                        if (c.getCanteenId().equals(canteenId)) {
                            canteenName = c.getName();
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        canteenId = canteens.get(0).getCanteenId();
                        canteenName = canteens.get(0).getName();
                    }
                }
                updateActiveBarTitles(cName, canteenName);
            }

            @Override
            public void onError(Exception e) {
                updateActiveBarTitles("Stanford University Campus", "Byte Bites Cyber Cafe");
            }
        });
    }

    private void setupBottomNavigation() {
        navTabMenu.setOnClickListener(v -> switchToTab(true));
        navTabTokens.setOnClickListener(v -> switchToTab(false));

        btnSubTabLiveTokens.setOnClickListener(v -> {
            isShowingHistoryInTokens = false;
            updateSubTabsUi();
            loadTokensTabContent();
        });

        btnSubTabHistory.setOnClickListener(v -> {
            isShowingHistoryInTokens = true;
            updateSubTabsUi();
            loadTokensTabContent();
        });
    }

    private void updateSubTabsUi() {
        int primaryColor = getResources().getColor(R.color.primary);
        if (!isShowingHistoryInTokens) {
            if (tvSubTabLiveTokensTitle != null) tvSubTabLiveTokensTitle.setTextColor(Color.parseColor("#432874"));
            if (indicatorSubTabLiveTokens != null) {
                indicatorSubTabLiveTokens.setBackgroundColor(primaryColor);
                indicatorSubTabLiveTokens.setVisibility(View.VISIBLE);
            }
            if (tvSubTabHistoryTitle != null) tvSubTabHistoryTitle.setTextColor(Color.parseColor("#584C70"));
            if (indicatorSubTabHistory != null) {
                indicatorSubTabHistory.setBackgroundColor(Color.TRANSPARENT);
                indicatorSubTabHistory.setVisibility(View.INVISIBLE);
            }
        } else {
            if (tvSubTabLiveTokensTitle != null) tvSubTabLiveTokensTitle.setTextColor(Color.parseColor("#584C70"));
            if (indicatorSubTabLiveTokens != null) {
                indicatorSubTabLiveTokens.setBackgroundColor(Color.TRANSPARENT);
                indicatorSubTabLiveTokens.setVisibility(View.INVISIBLE);
            }
            if (tvSubTabHistoryTitle != null) tvSubTabHistoryTitle.setTextColor(Color.parseColor("#432874"));
            if (indicatorSubTabHistory != null) {
                indicatorSubTabHistory.setBackgroundColor(primaryColor);
                indicatorSubTabHistory.setVisibility(View.VISIBLE);
            }
        }
    }

    private void switchToTab(boolean isMenu) {
        if (isMenu) {
            layoutMenuTab.setVisibility(View.VISIBLE);
            layoutTokensTab.setVisibility(View.GONE);
            tvNavMenuLabel.setTextColor(getResources().getColor(R.color.primary));
            tvNavTokensLabel.setTextColor(getResources().getColor(R.color.on_surface_variant));
        } else {
            layoutMenuTab.setVisibility(View.GONE);
            layoutTokensTab.setVisibility(View.VISIBLE);
            tvNavMenuLabel.setTextColor(getResources().getColor(R.color.on_surface_variant));
            tvNavTokensLabel.setTextColor(getResources().getColor(R.color.primary));
            updateSubTabsUi();
            loadTokensTabContent();
        }
    }

    private void setupLiveOrderSection() {
        btnSimulateProgress.setText("Track Status");
        btnSimulateProgress.setOnClickListener(v -> {
            if (currentLiveOrder != null) {
                showOrderTrackerDialog(currentLiveOrder);
            }
        });
    }

    private void refreshLiveOrders() {
        syncOrdersWithKitchen(false);
    }

    private void syncOrdersWithKitchen(boolean userInitiated) {
        if (tvLiveSyncStatus != null && userInitiated) {
            tvLiveSyncStatus.setText("Syncing with Kitchen orders table...");
        }
        DatabaseExecutor.execute(() -> orderDao.getCustomerOrders(userId, userIdString), new DatabaseExecutor.Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> orders) {
                if (tvLiveSyncStatus != null) {
                    tvLiveSyncStatus.setText("Live synced with Kitchen orders table");
                }
                if (userInitiated) {
                    Toast.makeText(CustomerMenuActivity.this, "Synced with Kitchen orders table", Toast.LENGTH_SHORT).show();
                }

                if (orders == null || orders.isEmpty()) {
                    layoutLiveOrderSection.setVisibility(View.GONE);
                    return;
                }

                // Check for status transitions from kitchen
                boolean statusChanged = false;
                for (Order o : orders) {
                    String key = o.getIdString() != null ? o.getIdString() : o.getOrderNumber();
                    if (key == null) key = String.valueOf(o.getOrderId());
                    String currentStatus = o.getOrderStatus() != null ? o.getOrderStatus().toUpperCase() : "PLACED";
                    if (lastKnownStatusMap.containsKey(key)) {
                        String oldStatus = lastKnownStatusMap.get(key);
                        if (oldStatus != null && !oldStatus.equalsIgnoreCase(currentStatus)) {
                            statusChanged = true;
                            notifyUserOfStatusChange(o, oldStatus, currentStatus);
                        }
                    }
                    lastKnownStatusMap.put(key, currentStatus);
                }

                // Find active live order
                Order active = null;
                for (Order o : orders) {
                    String st = o.getOrderStatus();
                    if (!"COMPLETED".equalsIgnoreCase(st) && !"CANCELLED".equalsIgnoreCase(st)) {
                        active = o;
                        break;
                    }
                }
                if (active == null) active = orders.get(0);
                currentLiveOrder = active;
                updateLiveOrderUi(active);
                layoutLiveOrderSection.setVisibility(View.VISIBLE);

                // If Tokens tab is visible or status changed or manual sync, reload tokens tab
                if (layoutTokensTab.getVisibility() == View.VISIBLE || statusChanged || userInitiated) {
                    loadTokensTabContent();
                }

                // If tracker bottom sheet is open for this order, refresh its timeline live
                if (activeTrackerSheet != null && activeTrackerSheet.isShowing() && activeTrackerRoot != null && activeTrackerOrder != null) {
                    for (Order liveOrder : orders) {
                        if ((liveOrder.getIdString() != null && liveOrder.getIdString().equals(activeTrackerOrder.getIdString())) ||
                                (liveOrder.getOrderNumber() != null && liveOrder.getOrderNumber().equals(activeTrackerOrder.getOrderNumber()))) {
                            activeTrackerOrder = liveOrder;
                            updateTrackerTimeline(activeTrackerRoot, liveOrder.getOrderStatus());
                            break;
                        }
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (tvLiveSyncStatus != null) {
                    tvLiveSyncStatus.setText("Offline cache mode");
                }
            }
        });
    }

    private void notifyUserOfStatusChange(Order order, String oldStatus, String newStatus) {
        String tokenStr = order.getTokenString() != null ? order.getTokenString() : ("#" + order.getTokenNumber());
        if (!tokenStr.startsWith("#")) tokenStr = "#" + tokenStr;

        String msg;
        if ("READY_FOR_PICKUP".equalsIgnoreCase(newStatus) || "READY".equalsIgnoreCase(newStatus)) {
            msg = "🔔 " + tokenStr + " is READY FOR PICKUP at " + (order.getCanteenName() != null ? order.getCanteenName() : "Kitchen") + "! OTP: " + (order.getPickupOtp() != null ? order.getPickupOtp() : "1310");
        } else if ("PREPARING".equalsIgnoreCase(newStatus) || "COOKING".equalsIgnoreCase(newStatus)) {
            msg = "👨‍🍳 Kitchen started cooking order " + tokenStr + "!";
        } else if ("ACCEPTED".equalsIgnoreCase(newStatus)) {
            msg = "📋 Order " + tokenStr + " accepted by kitchen staff!";
        } else if ("COMPLETED".equalsIgnoreCase(newStatus)) {
            msg = "✅ Order " + tokenStr + " picked up / completed. Enjoy your meal!";
        } else {
            msg = "Order " + tokenStr + " status updated: " + newStatus;
        }

        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG)
                .setAction("View", v -> switchToTab(false))
                .show();
    }

    private void updateLiveOrderUi(Order o) {
        if (o == null) return;
        tvLiveTokenPill.setText(o.getTokenString() != null ? "#" + o.getTokenString() : "#TK-" + o.getTokenNumber());
        tvLiveOrderCanteen.setText(o.getCanteenName() != null ? o.getCanteenName() : canteenName);
        tvLiveOrderStatusBadge.setText(o.getOrderStatus().toUpperCase());

        String s = o.getOrderStatus().toUpperCase();
        if ("PLACED".equals(s)) {
            tvLiveOrderStepper.setText("● Placed ➔ ○ Accepted ➔ ○ Preparing ➔ ○ Ready for Pickup");
        } else if ("ACCEPTED".equals(s)) {
            tvLiveOrderStepper.setText("✔ Placed ➔ ● Accepted ➔ ○ Preparing ➔ ○ Ready for Pickup");
        } else if ("PREPARING".equals(s) || "COOKING".equals(s)) {
            tvLiveOrderStepper.setText("✔ Placed ➔ ✔ Accepted ➔ ● Preparing ➔ ○ Ready for Pickup");
        } else if ("READY".equals(s) || "READY_FOR_PICKUP".equals(s)) {
            tvLiveOrderStepper.setText("✔ Placed ➔ ✔ Accepted ➔ ✔ Preparing ➔ ★ READY FOR PICKUP!");
        } else {
            tvLiveOrderStepper.setText("✔ Completed & Picked Up");
        }
        btnSimulateProgress.setText("Track Status");
        btnSimulateProgress.setOnClickListener(v -> showOrderTrackerDialog(o));

        if (o.getItemsSummary() != null && !o.getItemsSummary().isEmpty()) {
            tvLiveOrderItemsSummary.setText(o.getItemsSummary());
        } else {
            tvLiveOrderItemsSummary.setText("Campus Meal Items");
        }

        BigDecimal amt = o.getFinalAmount() != null ? o.getFinalAmount() : o.getTotalAmount();
        tvLiveOrderPrice.setText(String.format("$%.2f (%s)", amt, o.getPaymentMethod() != null ? o.getPaymentMethod().replace("_", " ") : "Wallet"));
    }

    private void setupWalletSection() {
        tvWalletBalanceDisplay.setText(String.format("$%.2f", walletBalance));

        btnQuickAdd10.setOnClickListener(v -> topUpWallet(new BigDecimal("10.00")));
        btnQuickAdd25.setOnClickListener(v -> topUpWallet(new BigDecimal("25.00")));
        btnQuickAdd50.setOnClickListener(v -> topUpWallet(new BigDecimal("50.00")));

        btnTopUpWallet.setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint("Enter amount (e.g. 20.00)");
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Top Up Campus Wallet")
                    .setMessage("Add funds to your student meal card account:")
                    .setView(input)
                    .setPositiveButton("Top Up", (dialog, which) -> {
                        String txt = input.getText().toString().trim();
                        try {
                            BigDecimal val = new BigDecimal(txt);
                            if (val.compareTo(BigDecimal.ZERO) > 0) {
                                topUpWallet(val);
                            }
                        } catch (Exception ignored) {}
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void topUpWallet(BigDecimal amount) {
        walletBalance = walletBalance.add(amount);
        tvWalletBalanceDisplay.setText(String.format("$%.2f", walletBalance));

        // Persist to Cloud SQL in background
        DatabaseExecutor.execute(() -> userDao.updateWalletBalance(userId, null, walletBalance), null);

        // Add ledger record view
        View row = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, null);
        TextView t1 = row.findViewById(android.R.id.text1);
        TextView t2 = row.findViewById(android.R.id.text2);
        t1.setText("Wallet Top-Up (Instant UPI)");
        t1.setTextSize(11.5f);
        t2.setText(String.format("+$%.2f • Balance: $%.2f", amount, walletBalance));
        t2.setTextColor(Color.parseColor("#10B981"));
        t2.setTextSize(10.5f);
        layoutLedgerItems.addView(row, 0);

        Toast.makeText(this, String.format("Added $%.2f to Campus Wallet! New Balance: $%.2f", amount, walletBalance), Toast.LENGTH_SHORT).show();
    }

    private void setupSearchAndFilters() {
        etSearchDishes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                activeSearchQuery = s.toString().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnFilterAllCanteens.setOnClickListener(v -> selectStallFilter(0));
        btnFilterCanteen1.setOnClickListener(v -> selectStallFilter(1));
        btnFilterCanteen2.setOnClickListener(v -> selectStallFilter(2));

        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipAll) activeCategoryFilter = "All";
            else if (checkedId == R.id.chipFastFood) activeCategoryFilter = "Fast Food";
            else if (checkedId == R.id.chipMeals) activeCategoryFilter = "Meals";
            else if (checkedId == R.id.chipBowls) activeCategoryFilter = "Healthy Bowls";
            else if (checkedId == R.id.chipVeg) activeCategoryFilter = "Veg";
            else if (checkedId == R.id.chipBeverages) activeCategoryFilter = "Beverages";
            applyFilters();
        });
    }

    private void selectStallFilter(int index) {
        activeStallFilterIndex = index;
        int activeBg = getResources().getColor(R.color.primary);
        int inactiveColor = getResources().getColor(R.color.on_surface);

        if (index == 0) {
            btnFilterAllCanteens.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activeBg));
            btnFilterAllCanteens.setTextColor(Color.WHITE);
            btnFilterCanteen1.setBackgroundTintList(null);
            btnFilterCanteen1.setTextColor(inactiveColor);
            btnFilterCanteen2.setBackgroundTintList(null);
            btnFilterCanteen2.setTextColor(inactiveColor);
        } else if (index == 1) {
            btnFilterAllCanteens.setBackgroundTintList(null);
            btnFilterAllCanteens.setTextColor(inactiveColor);
            btnFilterCanteen1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activeBg));
            btnFilterCanteen1.setTextColor(Color.WHITE);
            btnFilterCanteen2.setBackgroundTintList(null);
            btnFilterCanteen2.setTextColor(inactiveColor);
        } else {
            btnFilterAllCanteens.setBackgroundTintList(null);
            btnFilterAllCanteens.setTextColor(inactiveColor);
            btnFilterCanteen1.setBackgroundTintList(null);
            btnFilterCanteen1.setTextColor(inactiveColor);
            btnFilterCanteen2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activeBg));
            btnFilterCanteen2.setTextColor(Color.WHITE);
        }
        applyFilters();
    }

    private void setupMenuRecyclerView() {
        menuAdapter = new CustomerMenuAdapter((cartMap, totalAmount) -> {
            currentTotal = totalAmount;
            int totalItems = 0;
            for (int qty : cartMap.values()) {
                totalItems += qty;
            }

            if (totalItems > 0) {
                cardCartBar.setVisibility(View.VISIBLE);
                tvCartBadge.setVisibility(View.VISIBLE);
                tvCartBadge.setText(String.valueOf(totalItems));
                tvCartSummary.setText(String.format("%d Items • $%.2f", totalItems, totalAmount));
                tvNextTokenPreview.setText("Tap to view tray & generate pickup token");
            } else {
                cardCartBar.setVisibility(View.GONE);
                tvCartBadge.setVisibility(View.GONE);
            }
        });

        rvCustomerMenu.setLayoutManager(new LinearLayoutManager(this));
        rvCustomerMenu.setAdapter(menuAdapter);
    }

    private void loadMenu() {
        progressBarMenu.setVisibility(View.VISIBLE);
        DatabaseExecutor.execute(() -> menuItemDao.getMenuItemsByCollege(collegeId, null), new DatabaseExecutor.Callback<List<MenuItem>>() {
            @Override
            public void onSuccess(List<MenuItem> items) {
                progressBarMenu.setVisibility(View.GONE);
                allMenuItems = items != null ? items : new ArrayList<>();
                applyFilters();
            }

            @Override
            public void onError(Exception e) {
                progressBarMenu.setVisibility(View.GONE);
                Toast.makeText(CustomerMenuActivity.this, "Failed to load menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        displayedMenuItems.clear();
        for (int i = 0; i < allMenuItems.size(); i++) {
            MenuItem item = allMenuItems.get(i);

            // Stall filter: Alternate between canteens for variety if single college
            if (activeStallFilterIndex == 1 && (i % 2 != 0)) continue;
            if (activeStallFilterIndex == 2 && (i % 2 == 0)) continue;

            // Category filter
            if (!"All".equalsIgnoreCase(activeCategoryFilter)) {
                if (item.getCategory() == null || !item.getCategory().equalsIgnoreCase(activeCategoryFilter)) {
                    continue;
                }
            }

            // Search query filter
            if (!activeSearchQuery.isEmpty()) {
                String q = activeSearchQuery.toLowerCase();
                boolean matchName = item.getName() != null && item.getName().toLowerCase().contains(q);
                boolean matchDesc = item.getDescription() != null && item.getDescription().toLowerCase().contains(q);
                boolean matchCat = item.getCategory() != null && item.getCategory().toLowerCase().contains(q);
                if (!matchName && !matchDesc && !matchCat) {
                    continue;
                }
            }

            displayedMenuItems.add(item);
        }

        tvMenuHeaderCount.setText(String.format("Canteen Menu (%d Items)", displayedMenuItems.size()));
        menuAdapter.setItems(displayedMenuItems);
    }

    /**
     * Renders Token Pass Cards in the My Tokens Tab (Screenshots 9 & 10)
     */
    private void loadTokensTabContent() {
        layoutTokensContainer.removeAllViews();
        DatabaseExecutor.execute(() -> orderDao.getCustomerOrders(userId, userIdString), new DatabaseExecutor.Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> orders) {
                int liveCount = 0;
                int historyCount = 0;

                if (orders != null) {
                    for (Order o : orders) {
                        boolean isDone = "COMPLETED".equalsIgnoreCase(o.getOrderStatus()) || "CANCELLED".equalsIgnoreCase(o.getOrderStatus());
                        if (isDone) {
                            historyCount++;
                        } else {
                            liveCount++;
                        }
                    }
                }

                if (tvSubTabLiveTokensTitle != null) {
                    tvSubTabLiveTokensTitle.setText("Live Tokens (" + liveCount + ")");
                }
                if (tvSubTabHistoryTitle != null) {
                    tvSubTabHistoryTitle.setText("Order History (" + historyCount + ")");
                }

                if (orders == null || orders.isEmpty()) {
                    TextView empty = new TextView(CustomerMenuActivity.this);
                    empty.setText("No orders placed yet.\nOrder something delicious to get your live token!");
                    empty.setGravity(android.view.Gravity.CENTER);
                    empty.setPadding(32, 64, 32, 32);
                    empty.setTextColor(getResources().getColor(R.color.on_surface_variant));
                    layoutTokensContainer.addView(empty);
                    return;
                }

                int count = 0;
                for (Order order : orders) {
                    boolean isCompleted = "COMPLETED".equalsIgnoreCase(order.getOrderStatus()) || "CANCELLED".equalsIgnoreCase(order.getOrderStatus());
                    if (isShowingHistoryInTokens != isCompleted) continue;

                    count++;
                    View card = LayoutInflater.from(CustomerMenuActivity.this).inflate(R.layout.item_token_pass_card, layoutTokensContainer, false);
                    TextView tvCardCanteen = card.findViewById(R.id.tvCardCanteenName);
                    View layoutCardStatusPill = card.findViewById(R.id.layoutCardStatusPill);
                    View dotCardStatus = card.findViewById(R.id.dotCardStatus);
                    TextView tvCardStatusText = card.findViewById(R.id.tvCardStatusText);
                    TextView tvCardToken = card.findViewById(R.id.tvCardTokenNumber);
                    TextView tvCardOrder = card.findViewById(R.id.tvCardOrderNumber);
                    TextView tvCardOtp = card.findViewById(R.id.tvCardPickupOtp);
                    TextView tvCardItems = card.findViewById(R.id.tvCardItemsSummary);
                    TextView tvCardPayment = card.findViewById(R.id.tvCardPaymentMethod);
                    TextView tvCardTotal = card.findViewById(R.id.tvCardTotal);
                    MaterialButton btnCardAction = card.findViewById(R.id.btnCardAction);

                    tvCardCanteen.setText(order.getCanteenName() != null ? order.getCanteenName() : canteenName);

                    String status = order.getOrderStatus() != null ? order.getOrderStatus().toUpperCase() : "PLACED";
                    applyStatusPill(layoutCardStatusPill, dotCardStatus, tvCardStatusText, status);

                    String tokStr = order.getTokenString() != null ? order.getTokenString() : String.valueOf(order.getTokenNumber());
                    if (!tokStr.startsWith("#")) {
                        tokStr = "#" + (tokStr.startsWith("TK-") ? tokStr : "TK-" + tokStr);
                    }
                    tvCardToken.setText(tokStr);

                    String ordNum = order.getOrderNumber() != null ? order.getOrderNumber() : ("ORD-STI-" + order.getOrderId());
                    tvCardOrder.setText("Order: " + ordNum);

                    tvCardOtp.setText("Pickup OTP: " + (order.getPickupOtp() != null ? order.getPickupOtp() : "1310"));

                    bindStepper(card, status);

                    if (order.getItemsSummary() != null && !order.getItemsSummary().isEmpty()) {
                        tvCardItems.setText(order.getItemsSummary());
                    } else {
                        tvCardItems.setText("Campus Meal Order");
                    }

                    String payMethod = order.getPaymentMethod();
                    if (payMethod != null && payMethod.equalsIgnoreCase("UPI")) {
                        tvCardPayment.setText("Paid via UPI / GPay / PhonePe");
                    } else if (payMethod != null && payMethod.equalsIgnoreCase("CASH_ON_PICKUP")) {
                        tvCardPayment.setText("Pay on Pickup / Cash");
                    } else if (payMethod != null) {
                        tvCardPayment.setText("Paid via " + payMethod.replace("_", " "));
                    } else {
                        tvCardPayment.setText("Paid via UPI / GPay / PhonePe");
                    }

                    BigDecimal amt = order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount();
                    tvCardTotal.setText(String.format("$%.2f", amt));

                    // Action button configuration: Campus students can only view order tracker / details
                    btnCardAction.setVisibility(View.VISIBLE);
                    btnCardAction.setText("Track Order");
                    btnCardAction.setOnClickListener(v -> showOrderTrackerDialog(order));

                    layoutTokensContainer.addView(card);
                }

                if (count == 0) {
                    TextView empty = new TextView(CustomerMenuActivity.this);
                    empty.setText(isShowingHistoryInTokens ? "No past order history yet." : "No active tokens. Place an order to generate one!");
                    empty.setGravity(android.view.Gravity.CENTER);
                    empty.setPadding(32, 64, 32, 32);
                    empty.setTextColor(getResources().getColor(R.color.on_surface_variant));
                    layoutTokensContainer.addView(empty);
                }
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    private void applyStatusPill(View layoutPill, View dotView, TextView tvText, String status) {
        int bgRes;
        int dotColor;
        int textColor;
        String label;

        switch (status.toUpperCase()) {
            case "ACCEPTED":
                label = "Accepted by Kitchen";
                dotColor = Color.parseColor("#D97706");
                textColor = Color.parseColor("#B45309");
                bgRes = Color.parseColor("#FEF3C7");
                break;
            case "PREPARING":
            case "COOKING":
                label = "Cooking in Kitchen";
                dotColor = Color.parseColor("#9333EA");
                textColor = Color.parseColor("#7E22CE");
                bgRes = Color.parseColor("#F3E8FF");
                break;
            case "READY":
            case "READY_FOR_PICKUP":
                label = "Ready for Pickup";
                dotColor = Color.parseColor("#10B981");
                textColor = Color.parseColor("#047857");
                bgRes = Color.parseColor("#D1FAE5");
                break;
            case "COMPLETED":
                label = "Completed / Picked Up";
                dotColor = Color.parseColor("#64748B");
                textColor = Color.parseColor("#475569");
                bgRes = Color.parseColor("#F1F5F9");
                break;
            case "CANCELLED":
                label = "Order Cancelled";
                dotColor = Color.parseColor("#EF4444");
                textColor = Color.parseColor("#DC2626");
                bgRes = Color.parseColor("#FEE2E2");
                break;
            case "PLACED":
            default:
                label = "Order Placed";
                dotColor = Color.parseColor("#0284C7");
                textColor = Color.parseColor("#0369A1");
                bgRes = Color.parseColor("#E0F2FE");
                break;
        }

        tvText.setText(label);
        tvText.setTextColor(textColor);

        GradientDrawable dotShape = new GradientDrawable();
        dotShape.setShape(GradientDrawable.OVAL);
        dotShape.setColor(dotColor);
        dotView.setBackground(dotShape);

        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setShape(GradientDrawable.RECTANGLE);
        pillBg.setCornerRadius(dpToPx(16));
        pillBg.setColor(bgRes);
        layoutPill.setBackground(pillBg);
    }

    private void bindStepper(View card, String status) {
        ImageView ivStep1 = card.findViewById(R.id.ivStep1);
        ImageView ivStep2 = card.findViewById(R.id.ivStep2);
        ImageView ivStep3 = card.findViewById(R.id.ivStep3);
        ImageView ivStep4 = card.findViewById(R.id.ivStep4);

        View line1 = card.findViewById(R.id.lineStep1to2);
        View line2 = card.findViewById(R.id.lineStep2to3);
        View line3 = card.findViewById(R.id.lineStep3to4);

        TextView tvStep1 = card.findViewById(R.id.tvStep1);
        TextView tvStep2 = card.findViewById(R.id.tvStep2);
        TextView tvStep3 = card.findViewById(R.id.tvStep3);
        TextView tvStep4 = card.findViewById(R.id.tvStep4);

        int activeColor = getResources().getColor(R.color.primary);
        int inactiveColor = Color.parseColor("#E2E8F0");
        int inactiveTextColor = Color.parseColor("#94A3B8");

        int level = 1; // 1 = Placed, 2 = Cooking, 3 = Ready, 4 = Done
        if ("ACCEPTED".equalsIgnoreCase(status) || "PREPARING".equalsIgnoreCase(status) || "COOKING".equalsIgnoreCase(status)) {
            level = 2;
        } else if ("READY".equalsIgnoreCase(status) || "READY_FOR_PICKUP".equalsIgnoreCase(status)) {
            level = 3;
        } else if ("COMPLETED".equalsIgnoreCase(status)) {
            level = 4;
        }

        // Step 1: Placed
        ivStep1.setImageResource(R.drawable.ic_check_circle);
        ivStep1.setColorFilter(activeColor);
        tvStep1.setTextColor(activeColor);

        // Line 1 & Step 2: Cooking
        if (level >= 2) {
            line1.setBackgroundColor(activeColor);
            ivStep2.setImageResource(R.drawable.ic_check_circle);
            ivStep2.setColorFilter(activeColor);
            tvStep2.setTextColor(activeColor);
        } else {
            line1.setBackgroundColor(inactiveColor);
            ivStep2.setImageResource(R.drawable.bg_step_circle_inactive);
            ivStep2.clearColorFilter();
            tvStep2.setTextColor(inactiveTextColor);
        }

        // Line 2 & Step 3: Ready
        if (level >= 3) {
            line2.setBackgroundColor(activeColor);
            ivStep3.setImageResource(R.drawable.ic_check_circle);
            ivStep3.setColorFilter(activeColor);
            tvStep3.setTextColor(activeColor);
        } else {
            line2.setBackgroundColor(inactiveColor);
            ivStep3.setImageResource(R.drawable.bg_step_circle_inactive);
            ivStep3.clearColorFilter();
            tvStep3.setTextColor(inactiveTextColor);
        }

        // Line 3 & Step 4: Done
        if (level >= 4) {
            line3.setBackgroundColor(activeColor);
            ivStep4.setImageResource(R.drawable.ic_check_circle);
            ivStep4.setColorFilter(activeColor);
            tvStep4.setTextColor(activeColor);
        } else {
            line3.setBackgroundColor(inactiveColor);
            ivStep4.setImageResource(R.drawable.bg_step_circle_inactive);
            ivStep4.clearColorFilter();
            tvStep4.setTextColor(inactiveTextColor);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void showCartBottomSheet() {
        Map<Integer, Integer> cart = menuAdapter.getCartMap();
        if (cart == null || cart.isEmpty()) {
            Toast.makeText(this, "Your cart tray is currently empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_cart_sheet, null);
        bottomSheet.setContentView(sheetView);

        RecyclerView rvCartItems = sheetView.findViewById(R.id.rvCartItems);
        TextView tvCartTrayItemCount = sheetView.findViewById(R.id.tvCartTrayItemCount);
        TextView tvCartBillSubtotal = sheetView.findViewById(R.id.tvCartBillSubtotal);
        TextView tvCartBillTotal = sheetView.findViewById(R.id.tvCartBillTotal);
        TextInputEditText etCartSpecialInstructions = sheetView.findViewById(R.id.etCartSpecialInstructions);
        ChipGroup chipGroupPayment = sheetView.findViewById(R.id.chipGroupPayment);
        MaterialButton btnCartPlaceOrder = sheetView.findViewById(R.id.btnCartPlaceOrder);
        View btnCloseCart = sheetView.findViewById(R.id.btnCloseCart);

        btnCloseCart.setOnClickListener(v -> bottomSheet.dismiss());

        CartAdapter cartAdapter = new CartAdapter((item, newQty) -> {
            menuAdapter.updateItemQuantity(item.getItemId(), newQty);
            Map<Integer, Integer> updatedCart = menuAdapter.getCartMap();
            if (updatedCart.isEmpty()) {
                bottomSheet.dismiss();
                return;
            }

            int count = 0;
            BigDecimal subtotal = BigDecimal.ZERO;
            for (Map.Entry<Integer, Integer> e : updatedCart.entrySet()) {
                count += e.getValue();
                MenuItem mi = menuAdapter.getItemById(e.getKey());
                if (mi != null) {
                    subtotal = subtotal.add(mi.getPrice().multiply(BigDecimal.valueOf(e.getValue())));
                }
            }

            tvCartTrayItemCount.setText(count + " Items");
            tvCartBillSubtotal.setText(String.format("$%.2f", subtotal));
            tvCartBillTotal.setText(String.format("$%.2f", subtotal));
            btnCartPlaceOrder.setText(String.format("Pay & Get Live Token ($%.2f)", subtotal));
        });

        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        rvCartItems.setAdapter(cartAdapter);
        cartAdapter.setCartData(cart, menuAdapter.getItemLookup());

        int totalItems = 0;
        for (int qty : cart.values()) totalItems += qty;
        tvCartTrayItemCount.setText(totalItems + " Items");
        tvCartBillSubtotal.setText(String.format("$%.2f", currentTotal));
        tvCartBillTotal.setText(String.format("$%.2f", currentTotal));
        btnCartPlaceOrder.setText(String.format("Pay & Get Live Token ($%.2f)", currentTotal));

        btnCartPlaceOrder.setOnClickListener(v -> {
            btnCartPlaceOrder.setEnabled(false);
            String note = etCartSpecialInstructions.getText() != null ? etCartSpecialInstructions.getText().toString().trim() : "";

            String paymentMethod = "STUDENT_WALLET";
            int checkedPaymentId = chipGroupPayment.getCheckedChipId();
            if (checkedPaymentId == R.id.chipUpi) {
                paymentMethod = "UPI_QR";
            } else if (checkedPaymentId == R.id.chipCash) {
                paymentMethod = "CASH_AT_COUNTER";
            }

            executeOrderPlacement(bottomSheet, note, paymentMethod);
        });

        bottomSheet.show();
    }

    private void executeOrderPlacement(BottomSheetDialog cartSheet, String instructions, String paymentMethod) {
        Map<Integer, Integer> cart = menuAdapter.getCartMap();
        if (cart == null || cart.isEmpty()) return;

        List<OrderItem> orderItems = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            int itemId = entry.getKey();
            int qty = entry.getValue();
            if (qty > 0) {
                MenuItem item = menuAdapter.getItemById(itemId);
                if (item != null) {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setItemId(itemId);
                    orderItem.setQuantity(qty);
                    orderItem.setPrice(item.getPrice());
                    orderItem.setItemName(item.getName());
                    orderItems.add(orderItem);
                }
            }
        }

        Order newOrder = new Order();
        newOrder.setUserId(userId);
        newOrder.setUserIdString(userIdString);
        newOrder.setCollegeId(collegeId);
        newOrder.setCollegeIdString("col_" + collegeId);
        newOrder.setUserName(userName);
        newOrder.setCanteenId(canteenId);
        newOrder.setCanteenName(canteenName);
        newOrder.setTotalAmount(currentTotal);
        newOrder.setFinalAmount(currentTotal);
        newOrder.setPaymentMethod(paymentMethod);
        newOrder.setPaymentStatus("SUCCESS");
        newOrder.setOrderStatus("PLACED");
        newOrder.setSpecialInstructions(instructions);

        // Deduct from wallet balance if wallet chosen
        if ("STUDENT_WALLET".equals(paymentMethod) && walletBalance.compareTo(currentTotal) >= 0) {
            walletBalance = walletBalance.subtract(currentTotal);
            tvWalletBalanceDisplay.setText(String.format("$%.2f", walletBalance));
            DatabaseExecutor.execute(() -> userDao.updateWalletBalance(userId, null, walletBalance), null);
        }

        Toast.makeText(this, "Generating live token & syncing with kitchen...", Toast.LENGTH_SHORT).show();

        DatabaseExecutor.execute(() -> orderDao.createOrder(newOrder, orderItems), new DatabaseExecutor.Callback<Integer>() {
            @Override
            public void onSuccess(Integer orderId) {
                if (cartSheet != null && cartSheet.isShowing()) {
                    cartSheet.dismiss();
                }
                menuAdapter.clearCart();
                refreshLiveOrders();
                showOrderTrackerDialog(newOrder);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(CustomerMenuActivity.this, "Order failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showOrderTrackerDialog(Order order) {
        if (order == null) return;

        BottomSheetDialog trackerSheet = new BottomSheetDialog(this);
        View trackerRoot = LayoutInflater.from(this).inflate(R.layout.dialog_order_tracker, null);
        trackerSheet.setContentView(trackerRoot);

        TextView tvTrackerOrderNumber = trackerRoot.findViewById(R.id.tvTrackerOrderNumber);
        TextView tvTrackerTokenBig = trackerRoot.findViewById(R.id.tvTrackerTokenBig);
        TextView tvTrackerPinBig = trackerRoot.findViewById(R.id.tvTrackerPinBig);
        TextView tvTrackerItemsList = trackerRoot.findViewById(R.id.tvTrackerItemsList);
        TextView tvTrackerPaymentInfo = trackerRoot.findViewById(R.id.tvTrackerPaymentInfo);
        TextView tvTrackerTotalAmount = trackerRoot.findViewById(R.id.tvTrackerTotalAmount);
        View btnTrackerRefresh = trackerRoot.findViewById(R.id.btnTrackerRefresh);
        View btnTrackerClose = trackerRoot.findViewById(R.id.btnTrackerClose);
        View btnTrackerDone = trackerRoot.findViewById(R.id.btnTrackerDone);

        tvTrackerOrderNumber.setText(order.getOrderNumber() != null ? "Order #" + order.getOrderNumber() : "Order #" + order.getOrderId());
        tvTrackerTokenBig.setText(order.getTokenString() != null ? "#" + order.getTokenString() : "#TK-" + order.getTokenNumber());
        tvTrackerPinBig.setText(order.getPickupOtp() != null ? order.getPickupOtp() : "1310");

        if (order.getItemsSummary() != null && !order.getItemsSummary().isEmpty()) {
            tvTrackerItemsList.setText("• " + order.getItemsSummary().replace(", ", "\n• "));
        } else if (order.getItems() != null && !order.getItems().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (OrderItem item : order.getItems()) {
                sb.append("• ").append(item.getQuantity()).append("x ").append(item.getItemName()).append("\n");
            }
            tvTrackerItemsList.setText(sb.toString().trim());
        } else {
            tvTrackerItemsList.setText("• Campus Meal Items");
        }

        tvTrackerPaymentInfo.setText("Paid via " + (order.getPaymentMethod() != null ? order.getPaymentMethod().replace("_", " ") : "Wallet"));
        BigDecimal amt = order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount();
        tvTrackerTotalAmount.setText(String.format("$%.2f", amt));

        updateTrackerTimeline(trackerRoot, order.getOrderStatus());

        activeTrackerSheet = trackerSheet;
        activeTrackerRoot = trackerRoot;
        activeTrackerOrder = order;

        trackerSheet.setOnDismissListener(dialog -> {
            if (activeTrackerSheet == trackerSheet) {
                activeTrackerSheet = null;
                activeTrackerRoot = null;
                activeTrackerOrder = null;
            }
        });

        btnTrackerRefresh.setOnClickListener(v -> {
            DatabaseExecutor.execute(() -> orderDao.getCustomerOrders(userId, userIdString), new DatabaseExecutor.Callback<List<Order>>() {
                @Override
                public void onSuccess(List<Order> orders) {
                    if (orders != null) {
                        for (Order liveOrder : orders) {
                            if ((liveOrder.getIdString() != null && liveOrder.getIdString().equals(order.getIdString())) ||
                                    (liveOrder.getOrderNumber() != null && liveOrder.getOrderNumber().equals(order.getOrderNumber())) ||
                                    liveOrder.getOrderId() == order.getOrderId()) {
                                updateTrackerTimeline(trackerRoot, liveOrder.getOrderStatus());
                                Toast.makeText(CustomerMenuActivity.this, "Live Kitchen Status: " + liveOrder.getOrderStatus(), Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onError(Exception e) {}
            });
        });

        btnTrackerClose.setOnClickListener(v -> trackerSheet.dismiss());
        btnTrackerDone.setOnClickListener(v -> trackerSheet.dismiss());

        trackerSheet.show();
    }

    private void updateTrackerTimeline(View root, String statusRaw) {
        String s = statusRaw != null ? statusRaw.toUpperCase() : "PLACED";

        ImageView ivStepPlacedIcon = root.findViewById(R.id.ivStepPlacedIcon);
        TextView tvStepPlacedTitle = root.findViewById(R.id.tvStepPlacedTitle);
        View line1to2 = root.findViewById(R.id.lineStep1to2);

        ImageView ivStepPreparingIcon = root.findViewById(R.id.ivStepPreparingIcon);
        TextView tvStepPreparingTitle = root.findViewById(R.id.tvStepPreparingTitle);
        View line2to3 = root.findViewById(R.id.lineStep2to3);

        ImageView ivStepReadyIcon = root.findViewById(R.id.ivStepReadyIcon);
        TextView tvStepReadyTitle = root.findViewById(R.id.tvStepReadyTitle);
        View line3to4 = root.findViewById(R.id.lineStep3to4);

        ImageView ivStepCompletedIcon = root.findViewById(R.id.ivStepCompletedIcon);
        TextView tvStepCompletedTitle = root.findViewById(R.id.tvStepCompletedTitle);

        int activeColor = getResources().getColor(R.color.primary);
        int inactiveColor = Color.parseColor("#94A3B8");
        int dividerActive = getResources().getColor(R.color.primary);
        int dividerInactive = Color.parseColor("#E2E8F0");

        ivStepPlacedIcon.setColorFilter(activeColor);
        tvStepPlacedTitle.setTextColor(activeColor);

        if ("PREPARING".equals(s) || "ACCEPTED".equals(s)) {
            line1to2.setBackgroundColor(dividerActive);
            ivStepPreparingIcon.setColorFilter(activeColor);
            tvStepPreparingTitle.setTextColor(activeColor);

            line2to3.setBackgroundColor(dividerInactive);
            ivStepReadyIcon.setColorFilter(inactiveColor);
            tvStepReadyTitle.setTextColor(inactiveColor);

            line3to4.setBackgroundColor(dividerInactive);
            ivStepCompletedIcon.setColorFilter(inactiveColor);
            tvStepCompletedTitle.setTextColor(inactiveColor);
        } else if ("READY".equals(s) || "READY_FOR_PICKUP".equals(s)) {
            line1to2.setBackgroundColor(dividerActive);
            ivStepPreparingIcon.setColorFilter(activeColor);
            tvStepPreparingTitle.setTextColor(activeColor);

            line2to3.setBackgroundColor(dividerActive);
            ivStepReadyIcon.setColorFilter(Color.parseColor("#E65100"));
            tvStepReadyTitle.setTextColor(Color.parseColor("#E65100"));
            tvStepReadyTitle.setText("3. READY FOR PICKUP! (Collect now)");

            line3to4.setBackgroundColor(dividerInactive);
            ivStepCompletedIcon.setColorFilter(inactiveColor);
            tvStepCompletedTitle.setTextColor(inactiveColor);
        } else if ("COMPLETED".equals(s)) {
            line1to2.setBackgroundColor(dividerActive);
            ivStepPreparingIcon.setColorFilter(activeColor);
            tvStepPreparingTitle.setTextColor(activeColor);

            line2to3.setBackgroundColor(dividerActive);
            ivStepReadyIcon.setColorFilter(activeColor);
            tvStepReadyTitle.setTextColor(activeColor);

            line3to4.setBackgroundColor(dividerActive);
            ivStepCompletedIcon.setColorFilter(activeColor);
            tvStepCompletedTitle.setTextColor(activeColor);
        } else {
            line1to2.setBackgroundColor(dividerInactive);
            ivStepPreparingIcon.setColorFilter(inactiveColor);
            tvStepPreparingTitle.setTextColor(inactiveColor);

            line2to3.setBackgroundColor(dividerInactive);
            ivStepReadyIcon.setColorFilter(inactiveColor);
            tvStepReadyTitle.setTextColor(inactiveColor);

            line3to4.setBackgroundColor(dividerInactive);
            ivStepCompletedIcon.setColorFilter(inactiveColor);
            tvStepCompletedTitle.setTextColor(inactiveColor);
        }
    }
}
