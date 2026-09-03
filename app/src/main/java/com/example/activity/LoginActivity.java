package com.example.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.R;
import com.example.db.CanteenDao;
import com.example.db.CollegeDao;
import com.example.db.DBConnection;
import com.example.db.DBInit;
import com.example.db.DatabaseExecutor;
import com.example.db.UserDao;
import com.example.model.Canteen;
import com.example.model.College;
import com.example.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private MaterialButton btnSwitchStudent, btnSwitchStaff;
    private LinearLayout layoutStaffRoles, layoutCustomerRegister;
    private MaterialCardView cardStaffNotice;
    private ChipGroup chipGroupRoles;
    private TextView tvRoleHint, tvDbStatus, tvRegisterLink;
    private MaterialButton btnQuickFill, btnLogin, btnDemoStudent, btnDemoKitchen, btnDemoCollege, btnDemoSuper;
    private TextInputEditText etEmail, etPassword;
    private ProgressBar progressBarLogin;
    private MaterialCardView btnThemeToggleLogin;
    private android.widget.ImageView ivThemeIconLogin;

    // College & Canteen selection fields for staff/admins
    private LinearLayout layoutStaffInstitution, layoutLoginCanteenContainer;
    private TextView tvLabelLoginCollege, tvLoginSelectedCollegeName, tvLoginSelectedCollegeSub;
    private MaterialCardView cardLoginCollegePicker;
    private TextView tvLabelLoginCanteen, tvLoginSelectedCanteenName, tvLoginSelectedCanteenSub;
    private MaterialCardView cardLoginCanteenPicker;

    private final UserDao userDao = new UserDao();
    private final CollegeDao collegeDao = new CollegeDao();
    private final CanteenDao canteenDao = new CanteenDao();
    private final List<College> availableColleges = new ArrayList<>();
    private final List<Canteen> availableCanteens = new ArrayList<>();
    private int selectedCollegeIndex = 0;
    private int selectedCanteenIndex = 0;

    private boolean isStaffPortal = false;
    private String selectedRoleCategory = "CUSTOMER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.util.ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupThemeToggle();
        setupPortalSwitch();
        setupRoleListeners();
        setupActions();

        // Default to Campus User Portal
        setPortalMode(false);

        // Load colleges & canteens for staff assignment
        loadStaffCollegesAndCanteens();

        // Background Cloud SQL connectivity probe & schema sync
        DBInit.initializeDatabaseAsync(this);
        checkDatabaseConnectivity();
    }

    private void setupThemeToggle() {
        btnThemeToggleLogin = findViewById(R.id.btnThemeToggleLogin);
        ivThemeIconLogin = findViewById(R.id.ivThemeIconLogin);
        if (btnThemeToggleLogin != null && ivThemeIconLogin != null) {
            boolean isDark = com.example.util.ThemeHelper.isDarkMode(this);
            ivThemeIconLogin.setImageResource(isDark ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
            btnThemeToggleLogin.setOnClickListener(v -> {
                com.example.util.ThemeHelper.toggleTheme(LoginActivity.this);
                recreate();
            });
        }
    }

    private void initViews() {
        btnSwitchStudent = findViewById(R.id.btnSwitchStudent);
        btnSwitchStaff = findViewById(R.id.btnSwitchStaff);
        layoutStaffRoles = findViewById(R.id.layoutStaffRoles);
        layoutCustomerRegister = findViewById(R.id.layoutCustomerRegister);
        cardStaffNotice = findViewById(R.id.cardStaffNotice);
        chipGroupRoles = findViewById(R.id.chipGroupRoles);
        tvRoleHint = findViewById(R.id.tvRoleHint);
        tvDbStatus = findViewById(R.id.tvDbStatus);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        btnQuickFill = findViewById(R.id.btnQuickFill);
        btnLogin = findViewById(R.id.btnLogin);
        btnDemoStudent = findViewById(R.id.btnDemoStudent);
        btnDemoKitchen = findViewById(R.id.btnDemoKitchen);
        btnDemoCollege = findViewById(R.id.btnDemoCollege);
        btnDemoSuper = findViewById(R.id.btnDemoSuper);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        progressBarLogin = findViewById(R.id.progressBarLogin);

        // Staff institution & canteen selection components
        layoutStaffInstitution = findViewById(R.id.layoutStaffInstitution);
        tvLabelLoginCollege = findViewById(R.id.tvLabelLoginCollege);
        cardLoginCollegePicker = findViewById(R.id.cardLoginCollegePicker);
        tvLoginSelectedCollegeName = findViewById(R.id.tvLoginSelectedCollegeName);
        tvLoginSelectedCollegeSub = findViewById(R.id.tvLoginSelectedCollegeSub);

        layoutLoginCanteenContainer = findViewById(R.id.layoutLoginCanteenContainer);
        tvLabelLoginCanteen = findViewById(R.id.tvLabelLoginCanteen);
        cardLoginCanteenPicker = findViewById(R.id.cardLoginCanteenPicker);
        tvLoginSelectedCanteenName = findViewById(R.id.tvLoginSelectedCanteenName);
        tvLoginSelectedCanteenSub = findViewById(R.id.tvLoginSelectedCanteenSub);

        if (cardLoginCollegePicker != null) {
            cardLoginCollegePicker.setOnClickListener(v -> showCollegePicker());
        }
        if (cardLoginCanteenPicker != null) {
            cardLoginCanteenPicker.setOnClickListener(v -> showCanteenPicker());
        }
    }

    private void setupPortalSwitch() {
        btnSwitchStudent.setOnClickListener(v -> setPortalMode(false));
        btnSwitchStaff.setOnClickListener(v -> setPortalMode(true));
    }

    /**
     * Toggles between Campus User Portal (Login + Registration)
     * and Staff/Admin Portal (Strictly Login-Only, No Registration).
     */
    private void setPortalMode(boolean staffMode) {
        isStaffPortal = staffMode;
        int primaryColor = ContextCompat.getColor(this, R.color.primary);
        int onSurfaceVariant = ContextCompat.getColor(this, R.color.on_surface_variant);

        if (!staffMode) {
            // Campus User Portal Active
            selectedRoleCategory = "CUSTOMER";
            btnSwitchStudent.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            btnSwitchStudent.setTextColor(Color.WHITE);
            btnSwitchStudent.setElevation(4f);

            btnSwitchStaff.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            btnSwitchStaff.setTextColor(onSurfaceVariant);
            btnSwitchStaff.setElevation(0f);

            layoutStaffRoles.setVisibility(View.GONE);
            cardStaffNotice.setVisibility(View.GONE);
            if (layoutStaffInstitution != null) {
                layoutStaffInstitution.setVisibility(View.GONE);
            }

            // Registration explicitly available for campus users
            layoutCustomerRegister.setVisibility(View.VISIBLE);

            // Demo pills
            btnDemoStudent.setVisibility(View.VISIBLE);
            btnDemoKitchen.setVisibility(View.GONE);
            btnDemoCollege.setVisibility(View.GONE);
            btnDemoSuper.setVisibility(View.GONE);

            tvRoleHint.setText("Pre-order meals, track live kitchen tokens & skip lines");
            btnLogin.setText("Sign In as Campus User");

            etEmail.setText("alex.rivera@campus.edu");
            etPassword.setText("Student@123");
        } else {
            // Staff & Admin Portal Active
            btnSwitchStaff.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            btnSwitchStaff.setTextColor(Color.WHITE);
            btnSwitchStaff.setElevation(4f);

            btnSwitchStudent.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            btnSwitchStudent.setTextColor(onSurfaceVariant);
            btnSwitchStudent.setElevation(0f);

            layoutStaffRoles.setVisibility(View.VISIBLE);
            cardStaffNotice.setVisibility(View.VISIBLE);

            // REGISTRATION IS DISABLED FOR STAFF & ADMIN
            layoutCustomerRegister.setVisibility(View.GONE);

            // Demo pills
            btnDemoStudent.setVisibility(View.GONE);
            btnDemoKitchen.setVisibility(View.VISIBLE);
            btnDemoCollege.setVisibility(View.VISIBLE);
            btnDemoSuper.setVisibility(View.VISIBLE);

            btnLogin.setText("Sign In to Staff Console");

            // Apply selected staff chip
            applyActiveStaffRole();
        }
    }

    private void applyActiveStaffRole() {
        int checkedId = chipGroupRoles.getCheckedChipId();
        if (checkedId == R.id.chipCollegeAdmin) {
            selectedRoleCategory = "COLLEGE_ADMIN";
            tvRoleHint.setText("College menu items, campus canteen & inventory oversight");
            etEmail.setText("dean.harrison@stanford.edu");
            etPassword.setText("Admin@123");

            // College Admin can select college for that respective college only
            if (layoutStaffInstitution != null) {
                layoutStaffInstitution.setVisibility(View.VISIBLE);
            }
            if (tvLabelLoginCollege != null) {
                tvLabelLoginCollege.setText("Select College (Respective College Only) *");
            }
            if (layoutLoginCanteenContainer != null) {
                layoutLoginCanteenContainer.setVisibility(View.GONE);
            }
        } else if (checkedId == R.id.chipSuperAdmin) {
            selectedRoleCategory = "SUPER_ADMIN";
            tvRoleHint.setText("Multi-campus governance, colleges & system accounts");
            etEmail.setText("sarah.vance@appetite.io");
            etPassword.setText("Admin@123");

            if (layoutStaffInstitution != null) {
                layoutStaffInstitution.setVisibility(View.GONE);
            }
        } else {
            selectedRoleCategory = "KITCHEN_STAFF";
            chipGroupRoles.check(R.id.chipKitchen);
            tvRoleHint.setText("Live queue dashboard, order prep & token dispatch");
            etEmail.setText("chef.roberto@bytebites.edu");
            etPassword.setText("Kitchen@123");

            // Canteen Staff selects both college and canteen of that college
            if (layoutStaffInstitution != null) {
                layoutStaffInstitution.setVisibility(View.VISIBLE);
            }
            if (tvLabelLoginCollege != null) {
                tvLabelLoginCollege.setText("1. College / Institution *");
            }
            if (layoutLoginCanteenContainer != null) {
                layoutLoginCanteenContainer.setVisibility(View.VISIBLE);
            }
            if (tvLabelLoginCanteen != null) {
                tvLabelLoginCanteen.setText("2. Canteen of that College *");
            }
        }
    }

    private void loadStaffCollegesAndCanteens() {
        DatabaseExecutor.execute(() -> collegeDao.getAllColleges(), new DatabaseExecutor.Callback<List<College>>() {
            @Override
            public void onSuccess(List<College> colleges) {
                availableColleges.clear();
                if (colleges != null && !colleges.isEmpty()) {
                    availableColleges.addAll(colleges);
                }
                updateCollegeDisplay();
                loadCanteensForSelectedCollege();
            }

            @Override
            public void onError(Exception e) {
                updateCollegeDisplay();
            }
        });
    }

    public void selectCollegeByCode(String code) {
        if (code == null) return;
        String shortCode = College.toShortCode(code, null, null);
        for (int i = 0; i < availableColleges.size(); i++) {
            College c = availableColleges.get(i);
            if (c.getIdString().equalsIgnoreCase(shortCode) || College.toShortCode(c.getIdString(), c.getCollegeId(), c.getName()).equalsIgnoreCase(shortCode)) {
                selectedCollegeIndex = i;
                updateCollegeDisplay();
                loadCanteensForSelectedCollege();
                return;
            }
        }
    }

    private void updateCollegeDisplay() {
        if (tvLoginSelectedCollegeName == null) return;
        if (!availableColleges.isEmpty() && selectedCollegeIndex < availableColleges.size()) {
            College c = availableColleges.get(selectedCollegeIndex);
            String code = c.getIdString();
            tvLoginSelectedCollegeName.setText(c.getName() + " [" + code + "]");
            if (tvLoginSelectedCollegeSub != null) {
                tvLoginSelectedCollegeSub.setText("Code: " + code + " • " + (c.getLocation() != null ? c.getLocation() : "Campus Main"));
            }
        } else {
            tvLoginSelectedCollegeName.setText("Stanford University Campus [STAN]");
            if (tvLoginSelectedCollegeSub != null) {
                tvLoginSelectedCollegeSub.setText("Code: STAN • Main Campus, Building A");
            }
        }
    }

    private void loadCanteensForSelectedCollege() {
        String collegeIdStr = "STAN";
        if (!availableColleges.isEmpty() && selectedCollegeIndex < availableColleges.size()) {
            collegeIdStr = availableColleges.get(selectedCollegeIndex).getIdString();
        }
        final String finalCollegeIdStr = collegeIdStr;

        DatabaseExecutor.execute(() -> canteenDao.getCanteensForCollege(finalCollegeIdStr), new DatabaseExecutor.Callback<List<Canteen>>() {
            @Override
            public void onSuccess(List<Canteen> canteens) {
                availableCanteens.clear();
                if (canteens != null && !canteens.isEmpty()) {
                    availableCanteens.addAll(canteens);
                }
                selectedCanteenIndex = 0;
                updateCanteenDisplay();
            }

            @Override
            public void onError(Exception e) {
                updateCanteenDisplay();
            }
        });
    }

    private void updateCanteenDisplay() {
        if (tvLoginSelectedCanteenName == null) return;
        if (!availableCanteens.isEmpty() && selectedCanteenIndex < availableCanteens.size()) {
            Canteen c = availableCanteens.get(selectedCanteenIndex);
            tvLoginSelectedCanteenName.setText(c.getName());
            if (tvLoginSelectedCanteenSub != null) {
                tvLoginSelectedCanteenSub.setText(c.getLocation() != null ? c.getLocation() : "Canteen Stall");
            }
        } else {
            tvLoginSelectedCanteenName.setText("Byte Bites Canteen (Engineering Block)");
            if (tvLoginSelectedCanteenSub != null) {
                tvLoginSelectedCanteenSub.setText("Ground Floor, Engineering Wing");
            }
        }
    }

    private void showCollegePicker() {
        if (availableColleges.isEmpty()) {
            Toast.makeText(this, "Loading colleges...", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] collegeNames = new String[availableColleges.size()];
        for (int i = 0; i < availableColleges.size(); i++) {
            College c = availableColleges.get(i);
            collegeNames[i] = "[" + c.getIdString() + "] " + c.getName() + " (" + (c.getLocation() != null ? c.getLocation() : "Campus") + ")";
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select College / Institution")
                .setSingleChoiceItems(collegeNames, selectedCollegeIndex, (dialog, which) -> {
                    selectedCollegeIndex = which;
                    updateCollegeDisplay();
                    loadCanteensForSelectedCollege();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCanteenPicker() {
        if (availableCanteens.isEmpty()) {
            Toast.makeText(this, "Loading canteens for this college...", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] canteenNames = new String[availableCanteens.size()];
        for (int i = 0; i < availableCanteens.size(); i++) {
            Canteen c = availableCanteens.get(i);
            canteenNames[i] = c.getName() + " (" + (c.getLocation() != null ? c.getLocation() : "Stall") + ")";
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Canteen of College")
                .setSingleChoiceItems(canteenNames, selectedCanteenIndex, (dialog, which) -> {
                    selectedCanteenIndex = which;
                    updateCanteenDisplay();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupRoleListeners() {
        chipGroupRoles.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!isStaffPortal || checkedIds.isEmpty()) return;
            applyActiveStaffRole();
        });
    }

    private void setupActions() {
        btnQuickFill.setOnClickListener(v -> {
            if (!isStaffPortal) {
                etEmail.setText("alex.rivera@campus.edu");
                etPassword.setText("Student@123");
                selectCollegeByCode("STAN");
                Toast.makeText(this, "Autofilled Alex Rivera (STAN Student)", Toast.LENGTH_SHORT).show();
            } else if (chipGroupRoles.getCheckedChipId() == R.id.chipKitchen) {
                etEmail.setText("chef.roberto@bytebites.edu");
                etPassword.setText("Kitchen@123");
                selectCollegeByCode("STAN");
                Toast.makeText(this, "Autofilled Chef Roberto (STAN Kitchen Staff)", Toast.LENGTH_SHORT).show();
            } else if (chipGroupRoles.getCheckedChipId() == R.id.chipCollegeAdmin) {
                etEmail.setText("dean.harrison@stanford.edu");
                etPassword.setText("Admin@123");
                selectCollegeByCode("STAN");
                Toast.makeText(this, "Autofilled Dean Harrison (STAN College Admin)", Toast.LENGTH_SHORT).show();
            } else {
                etEmail.setText("sarah.vance@appetite.io");
                etPassword.setText("Admin@123");
                Toast.makeText(this, "Autofilled Sarah Vance (Super Admin)", Toast.LENGTH_SHORT).show();
            }
        });

        btnDemoStudent.setOnClickListener(v -> {
            setPortalMode(false);
            etEmail.setText("alex.rivera@campus.edu");
            etPassword.setText("Student@123");
            selectCollegeByCode("STAN");
            Toast.makeText(this, "Selected Alex Rivera (Stanford - STAN)", Toast.LENGTH_SHORT).show();
        });

        btnDemoKitchen.setOnClickListener(v -> {
            setPortalMode(true);
            chipGroupRoles.check(R.id.chipKitchen);
            etEmail.setText("chef.roberto@bytebites.edu");
            etPassword.setText("Kitchen@123");
            selectCollegeByCode("STAN");
            Toast.makeText(this, "Selected Chef Roberto (Stanford - STAN)", Toast.LENGTH_SHORT).show();
        });

        btnDemoCollege.setOnClickListener(v -> {
            setPortalMode(true);
            chipGroupRoles.check(R.id.chipCollegeAdmin);
            etEmail.setText("dean.harrison@stanford.edu");
            etPassword.setText("Admin@123");
            selectCollegeByCode("STAN");
            Toast.makeText(this, "Selected Dean Harrison (Stanford - STAN)", Toast.LENGTH_SHORT).show();
        });

        btnDemoSuper.setOnClickListener(v -> {
            setPortalMode(true);
            chipGroupRoles.check(R.id.chipSuperAdmin);
            etEmail.setText("sarah.vance@appetite.io");
            etPassword.setText("Admin@123");
            Toast.makeText(this, "Selected Sarah Vance (Super Admin)", Toast.LENGTH_SHORT).show();
        });

        tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (email.isEmpty()) {
            etEmail.setError("Please enter your email");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Please enter your password");
            return;
        }

        setLoading(true);

        // Execute JDBC query off the main UI thread via DatabaseExecutor
        DatabaseExecutor.execute(() -> userDao.authenticateUser(email, password), new DatabaseExecutor.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    handleStaffAssignmentAndRoute(user);
                } else {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Invalid email or password. Please verify your credentials.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Connection error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleStaffAssignmentAndRoute(User user) {
        String role = user.getRole();
        College selCol = (!availableColleges.isEmpty() && selectedCollegeIndex < availableColleges.size())
                ? availableColleges.get(selectedCollegeIndex) : null;
        Canteen selCanteen = (!availableCanteens.isEmpty() && selectedCanteenIndex < availableCanteens.size())
                ? availableCanteens.get(selectedCanteenIndex) : null;

        // VERIFY COLLEGE FROM DATABASE FOR CANTEEN STAFF & COLLEGE ADMIN
        boolean isStaffOrAdmin = "KITCHEN_STAFF".equalsIgnoreCase(role)
                || "CANTEEN_STAFF".equalsIgnoreCase(role)
                || "COLLEGE_ADMIN".equalsIgnoreCase(role);

        if (isStaffOrAdmin) {
            String dbCollegeCode = College.toShortCode(user.getCollegeIdString(), user.getCollegeId(), null);
            int dbCollegeNum = College.codeToNumericId(dbCollegeCode);

            String selCollegeCode = selCol != null ? College.toShortCode(selCol.getIdString(), selCol.getCollegeId(), selCol.getName()) : "";
            int selCollegeNum = selCol != null ? selCol.getCollegeId() : 0;

            boolean collegeMatches = dbCollegeCode.equalsIgnoreCase(selCollegeCode)
                    || (dbCollegeNum > 0 && dbCollegeNum == selCollegeNum);

            if (!collegeMatches) {
                setLoading(false);
                String assignedCollegeName = College.getCollegeDisplayName(dbCollegeCode);
                String selectedCollegeName = selCol != null ? selCol.getName() : "Selected College";

                new MaterialAlertDialogBuilder(this)
                        .setTitle("Access Declined: College Mismatch")
                        .setMessage("Login Denied for " + user.getEmail() + "!\n\n"
                                + "• Assigned College in Database:\n   " + assignedCollegeName + " [" + dbCollegeCode + "]\n\n"
                                + "• Selected College:\n   " + selectedCollegeName + " [" + selCollegeCode + "]\n\n"
                                + "Canteen Staff and College Admins can only log in under their designated college.")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Switch to " + dbCollegeCode, (dialog, which) -> {
                            selectCollegeByCode(dbCollegeCode);
                            Toast.makeText(this, "Switched selection to " + assignedCollegeName + " [" + dbCollegeCode + "]", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .setCancelable(false)
                        .show();
                return;
            }

            // Ensure the user object holds the verified canonical short code and numeric ID from database
            user.setCollegeIdString(dbCollegeCode);
            user.setCollegeId(dbCollegeNum);

            // For canteen staff, preserve their assigned canteen from database
            if ("KITCHEN_STAFF".equalsIgnoreCase(role) || "CANTEEN_STAFF".equalsIgnoreCase(role)) {
                if (user.getAssignedCanteenId() != null && !user.getAssignedCanteenId().trim().isEmpty()) {
                    for (Canteen c : availableCanteens) {
                        if (c.getCanteenId().equalsIgnoreCase(user.getAssignedCanteenId())) {
                            selCanteen = c;
                            break;
                        }
                    }
                }
            }
        }

        setLoading(false);
        routeUserByRole(user, selCol, selCanteen);
    }

    /**
     * Role-Based Navigation Logic:
     * - CUSTOMER -> CustomerMenuActivity (passes user_id, college_id, user_name, user_email)
     * - KITCHEN_STAFF -> KitchenDashboardActivity (passes college_id, canteen_id, canteen_name, user_name)
     * - COLLEGE_ADMIN -> CollegeAdminDashboardActivity (passes college_id, college_name, user_name)
     * - SUPER_ADMIN & System Roles -> SuperAdminDashboardActivity
     */
    private void routeUserByRole(User user, College selCol, Canteen selCanteen) {
        String role = user.getRole();
        Toast.makeText(this, "Signed in as " + user.getName() + " (" + role + ")", Toast.LENGTH_SHORT).show();

        Intent intent;
        if ("CUSTOMER".equalsIgnoreCase(role)) {
            intent = new Intent(this, CustomerMenuActivity.class);
            intent.putExtra("user_id", user.getUserId());
            intent.putExtra("user_id_str", user.getIdString());
            intent.putExtra("college_id", user.getCollegeId() != null ? user.getCollegeId() : 1);
            intent.putExtra("college_id_str", user.getCollegeIdString());
            intent.putExtra("user_name", user.getName());
            intent.putExtra("user_email", user.getEmail());
            intent.putExtra("canteen_id", user.getAssignedCanteenId());
        } else if ("KITCHEN_STAFF".equalsIgnoreCase(role) || "CANTEEN_STAFF".equalsIgnoreCase(role)) {
            intent = new Intent(this, KitchenDashboardActivity.class);
            intent.putExtra("user_id", user.getUserId());
            intent.putExtra("user_id_str", user.getIdString());
            intent.putExtra("college_id", user.getCollegeId() != null ? user.getCollegeId() : 1);
            intent.putExtra("college_id_str", user.getCollegeIdString());
            intent.putExtra("user_name", user.getName());
            intent.putExtra("canteen_id", user.getAssignedCanteenId() != null ? user.getAssignedCanteenId() : "canteen_1_1");
            intent.putExtra("canteen_name", selCanteen != null ? selCanteen.getName() : "Byte Bites Canteen");
            intent.putExtra("college_name", selCol != null ? selCol.getName() : "Stanford University Campus");
        } else if ("COLLEGE_ADMIN".equalsIgnoreCase(role)) {
            intent = new Intent(this, CollegeAdminDashboardActivity.class);
            intent.putExtra("user_id", user.getUserId());
            intent.putExtra("user_id_str", user.getIdString());
            intent.putExtra("college_id", user.getCollegeId() != null ? user.getCollegeId() : 1);
            intent.putExtra("college_id_str", user.getCollegeIdString());
            intent.putExtra("user_name", user.getName());
            intent.putExtra("college_name", selCol != null ? selCol.getName() : "Stanford University Campus");
            intent.putExtra("canteen_id", user.getAssignedCanteenId());
        } else {
            // SUPER_ADMIN, ROLE_ORDER, ROLE_CUSTOMER_DETAIL, ROLE_TRANSACTION
            intent = new Intent(this, SuperAdminDashboardActivity.class);
            intent.putExtra("user_id", user.getUserId());
            intent.putExtra("user_id_str", user.getIdString());
            intent.putExtra("user_name", user.getName());
            intent.putExtra("role", role);
        }

        startActivity(intent);
    }

    private void checkDatabaseConnectivity() {
        DatabaseExecutor.execute(() -> {
            boolean connected = DBConnection.getInstance().isConnected();
            DatabaseExecutor.runOnUiThread(() -> {
                if (connected) {
                    tvDbStatus.setText("Cloud SQL (34.100.184.249) - Online");
                } else {
                    tvDbStatus.setText("Cloud SQL Sync Engine - Ready");
                }
            });
        });
    }

    private void setLoading(boolean loading) {
        progressBarLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}
