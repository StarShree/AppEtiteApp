package com.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.R;
import com.example.db.CanteenDao;
import com.example.db.CollegeDao;
import com.example.db.DatabaseExecutor;
import com.example.db.UserDao;
import com.example.model.Canteen;
import com.example.model.College;
import com.example.model.User;
import com.example.util.ThemeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated Campus User Registration Activity.
 * Supports selecting among Colleges and their respective Canteens with high-visibility UI.
 */
public class RegisterActivity extends AppCompatActivity {

    private ImageButton btnBackRegister;
    private MaterialCardView btnThemeToggleRegister, cardCollegePicker, cardCanteenPicker;
    private ImageView ivThemeIconRegister;
    private TextView tvSelectedCollegeName, tvSelectedCollegeSub;
    private TextView tvSelectedCanteenName, tvCanteenLocationHint;
    private TextInputEditText etRegName, etRegRollNumber, etRegEmail, etRegPhone, etRegPassword, etRegConfirmPassword;
    private Spinner spinnerColleges, spinnerCanteens;
    private MaterialButton btnSubmitRegister;
    private ProgressBar progressBarRegister;
    private TextView tvBackToLoginLink;

    private final CollegeDao collegeDao = new CollegeDao();
    private final CanteenDao canteenDao = new CanteenDao();
    private final UserDao userDao = new UserDao();
    private final List<College> collegeList = new ArrayList<>();
    private final List<Canteen> currentCanteenList = new ArrayList<>();

    private int selectedCollegeIndex = 0;
    private int selectedCanteenIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply persisted light/dark theme
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupThemeToggle();
        setupPickers();
        loadColleges();
    }

    private void initViews() {
        btnBackRegister = findViewById(R.id.btnBackRegister);
        btnThemeToggleRegister = findViewById(R.id.btnThemeToggleRegister);
        ivThemeIconRegister = findViewById(R.id.ivThemeIconRegister);
        cardCollegePicker = findViewById(R.id.cardCollegePicker);
        cardCanteenPicker = findViewById(R.id.cardCanteenPicker);
        tvSelectedCollegeName = findViewById(R.id.tvSelectedCollegeName);
        tvSelectedCollegeSub = findViewById(R.id.tvSelectedCollegeSub);
        tvSelectedCanteenName = findViewById(R.id.tvSelectedCanteenName);
        tvCanteenLocationHint = findViewById(R.id.tvCanteenLocationHint);

        etRegName = findViewById(R.id.etRegName);
        etRegRollNumber = findViewById(R.id.etRegRollNumber);
        spinnerColleges = findViewById(R.id.spinnerColleges);
        spinnerCanteens = findViewById(R.id.spinnerCanteens);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPhone = findViewById(R.id.etRegPhone);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        btnSubmitRegister = findViewById(R.id.btnSubmitRegister);
        progressBarRegister = findViewById(R.id.progressBarRegister);
        tvBackToLoginLink = findViewById(R.id.tvBackToLoginLink);

        btnBackRegister.setOnClickListener(v -> finish());
        tvBackToLoginLink.setOnClickListener(v -> finish());
        btnSubmitRegister.setOnClickListener(v -> performRegistration());
    }

    private void setupThemeToggle() {
        boolean isDark = ThemeHelper.isDarkMode(this);
        ivThemeIconRegister.setImageResource(isDark ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);

        btnThemeToggleRegister.setOnClickListener(v -> {
            ThemeHelper.toggleTheme(RegisterActivity.this);
            recreate();
        });
    }

    private void setupPickers() {
        cardCollegePicker.setOnClickListener(v -> showCollegePickerDialog());
        cardCanteenPicker.setOnClickListener(v -> showCanteenPickerDialog());
    }

    private void loadColleges() {
        DatabaseExecutor.execute(collegeDao::getAllColleges, new DatabaseExecutor.Callback<List<College>>() {
            @Override
            public void onSuccess(List<College> colleges) {
                collegeList.clear();
                if (colleges != null && !colleges.isEmpty()) {
                    collegeList.addAll(colleges);
                }

                if (collegeList.isEmpty()) {
                    collegeList.add(new College("STAN", 1, "Stanford University Campus", "Main Campus, Building A"));
                    collegeList.add(new College("IMP", 2, "Imperial College of Engineering", "North Campus, Block A"));
                    collegeList.add(new College("SIT", 3, "St. Jude Institute of Technology", "South Campus, Central Plaza"));
                }

                selectedCollegeIndex = 0;
                updateSelectedCollegeUI();
                loadCanteensForCollege(collegeList.get(0).getIdString());
            }

            @Override
            public void onError(Exception e) {
                collegeList.clear();
                collegeList.add(new College("STAN", 1, "Stanford University Campus", "Main Campus, Building A"));
                collegeList.add(new College("IMP", 2, "Imperial College of Engineering", "North Campus, Block A"));
                collegeList.add(new College("SIT", 3, "St. Jude Institute of Technology", "South Campus, Central Plaza"));
                selectedCollegeIndex = 0;
                updateSelectedCollegeUI();
                loadCanteensForCollege(collegeList.get(0).getIdString());
            }
        });
    }

    private void updateSelectedCollegeUI() {
        if (!collegeList.isEmpty() && selectedCollegeIndex >= 0 && selectedCollegeIndex < collegeList.size()) {
            College c = collegeList.get(selectedCollegeIndex);
            tvSelectedCollegeName.setText(c.getName() + " [" + c.getIdString() + "]");
            tvSelectedCollegeSub.setText("Code: " + c.getIdString() + " • " + (c.getAddress() != null ? c.getAddress() : "Campus Location"));
        }
    }

    private void showCollegePickerDialog() {
        if (collegeList.isEmpty()) return;

        String[] items = new String[collegeList.size()];
        for (int i = 0; i < collegeList.size(); i++) {
            College c = collegeList.get(i);
            items[i] = "[" + c.getIdString() + "] " + c.getName() + "\n" + (c.getAddress() != null ? c.getAddress() : "");
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select College / Institution")
                .setSingleChoiceItems(items, selectedCollegeIndex, (dialog, which) -> {
                    selectedCollegeIndex = which;
                    updateSelectedCollegeUI();
                    dialog.dismiss();
                    College c = collegeList.get(which);
                    loadCanteensForCollege(c.getIdString() != null ? c.getIdString() : "STAN");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadCanteensForCollege(String collegeIdString) {
        DatabaseExecutor.execute(() -> canteenDao.getCanteensForCollege(collegeIdString), new DatabaseExecutor.Callback<List<Canteen>>() {
            @Override
            public void onSuccess(List<Canteen> canteens) {
                currentCanteenList.clear();
                if (canteens != null && !canteens.isEmpty()) {
                    currentCanteenList.addAll(canteens);
                }

                if (currentCanteenList.isEmpty()) {
                    currentCanteenList.add(new Canteen("canteen_1_1", "STAN", "Byte Bites Cyber Cafe", "Engineering Block, Level 1", "07:30 AM - 09:30 PM", "ACTIVE"));
                    currentCanteenList.add(new Canteen("canteen_1_2", "STAN", "Cardinal Quad Food Court", "Student Union Plaza", "08:00 AM - 10:00 PM", "ACTIVE"));
                }

                selectedCanteenIndex = 0;
                updateSelectedCanteenUI();
            }

            @Override
            public void onError(Exception e) {
                currentCanteenList.clear();
                currentCanteenList.add(new Canteen("canteen_1_1", "STAN", "Byte Bites Cyber Cafe", "Engineering Block, Level 1", "07:30 AM - 09:30 PM", "ACTIVE"));
                currentCanteenList.add(new Canteen("canteen_1_2", "STAN", "Cardinal Quad Food Court", "Student Union Plaza", "08:00 AM - 10:00 PM", "ACTIVE"));
                selectedCanteenIndex = 0;
                updateSelectedCanteenUI();
            }
        });
    }

    private void updateSelectedCanteenUI() {
        if (!currentCanteenList.isEmpty() && selectedCanteenIndex >= 0 && selectedCanteenIndex < currentCanteenList.size()) {
            Canteen c = currentCanteenList.get(selectedCanteenIndex);
            tvSelectedCanteenName.setText(c.getName());
            tvCanteenLocationHint.setText("📍 " + c.getLocation() + " • " + c.getOperatingHours());
        }
    }

    private void showCanteenPickerDialog() {
        if (currentCanteenList.isEmpty()) {
            Toast.makeText(this, "No canteens available for selected campus", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[currentCanteenList.size()];
        for (int i = 0; i < currentCanteenList.size(); i++) {
            Canteen c = currentCanteenList.get(i);
            items[i] = c.getName() + "\n" + c.getLocation() + " (" + c.getOperatingHours() + ")";
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Assigned Canteen Stall")
                .setSingleChoiceItems(items, selectedCanteenIndex, (dialog, which) -> {
                    selectedCanteenIndex = which;
                    updateSelectedCanteenUI();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performRegistration() {
        String name = etRegName.getText() != null ? etRegName.getText().toString().trim() : "";
        String rollNumber = etRegRollNumber.getText() != null ? etRegRollNumber.getText().toString().trim() : "";
        String email = etRegEmail.getText() != null ? etRegEmail.getText().toString().trim() : "";
        String phone = etRegPhone.getText() != null ? etRegPhone.getText().toString().trim() : "";
        String password = etRegPassword.getText() != null ? etRegPassword.getText().toString().trim() : "";
        String confirmPassword = etRegConfirmPassword.getText() != null ? etRegConfirmPassword.getText().toString().trim() : "";

        // Validations
        if (name.isEmpty()) {
            etRegName.setError("Full name is required");
            etRegName.requestFocus();
            return;
        }
        if (rollNumber.isEmpty()) {
            etRegRollNumber.setError("Campus / Roll ID number is required");
            etRegRollNumber.requestFocus();
            return;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegEmail.setError("Please enter a valid email address");
            etRegEmail.requestFocus();
            return;
        }
        if (phone.isEmpty()) {
            etRegPhone.setError("Phone number is required for pickup alerts");
            etRegPhone.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etRegPassword.setError("Password must be at least 6 characters");
            etRegPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etRegConfirmPassword.setError("Passwords do not match");
            etRegConfirmPassword.requestFocus();
            return;
        }

        College selectedCollege = (!collegeList.isEmpty() && selectedCollegeIndex < collegeList.size())
                ? collegeList.get(selectedCollegeIndex) : null;

        Canteen selectedCanteen = (!currentCanteenList.isEmpty() && selectedCanteenIndex < currentCanteenList.size())
                ? currentCanteenList.get(selectedCanteenIndex) : null;

        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPasswordHash(password);
        newUser.setRole("CUSTOMER");
        newUser.setStudentRollNumber(rollNumber);
        newUser.setCampusIdNumber(rollNumber);
        newUser.setPhoneNumber(phone);
        newUser.setWalletBalance(new BigDecimal("350.00"));
        newUser.setStatus("ACTIVE");
        newUser.setIdString("user_campus_" + System.currentTimeMillis());

        if (selectedCollege != null) {
            newUser.setCollegeId(selectedCollege.getCollegeId());
            newUser.setCollegeIdString(selectedCollege.getIdString());
        } else {
            newUser.setCollegeId(1);
            newUser.setCollegeIdString("STAN");
        }

        if (selectedCanteen != null) {
            newUser.setAssignedCanteenId(selectedCanteen.getCanteenId());
        } else {
            newUser.setAssignedCanteenId("canteen_1_1");
        }

        final College finalCollege = selectedCollege;
        final Canteen finalCanteen = selectedCanteen;

        setLoading(true);

        DatabaseExecutor.execute(() -> userDao.insertUser(newUser), new DatabaseExecutor.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                setLoading(false);
                if (Boolean.TRUE.equals(success)) {
                    String campusName = finalCollege != null ? finalCollege.getName() : "Campus";
                    String canName = finalCanteen != null ? finalCanteen.getName() : "Main Canteen";
                    Toast.makeText(RegisterActivity.this, "Registered at " + campusName + " (" + canName + ")! Welcome bonus $350.00 credited.", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(RegisterActivity.this, CustomerMenuActivity.class);
                    intent.putExtra("user_id", newUser.getUserId());
                    intent.putExtra("college_id", newUser.getCollegeId() != null ? newUser.getCollegeId() : 1);
                    intent.putExtra("college_id_str", newUser.getCollegeIdString());
                    intent.putExtra("user_name", newUser.getName());
                    intent.putExtra("user_email", newUser.getEmail());
                    intent.putExtra("canteen_id", newUser.getAssignedCanteenId());
                    intent.putExtra("canteen_name", canName);
                    startActivity(intent);
                    finish();
                } else {
                    String err = userDao.getLastErrorMessage();
                    if (err == null || err.isEmpty()) {
                        err = "Registration could not be completed. Please check your network connection and details.";
                    }
                    Toast.makeText(RegisterActivity.this, err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Registration error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBarRegister.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmitRegister.setEnabled(!loading);
    }
}
