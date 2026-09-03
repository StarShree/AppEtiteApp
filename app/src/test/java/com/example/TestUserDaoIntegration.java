package com.example;

import com.example.db.UserDao;
import com.example.model.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TestUserDaoIntegration {

    @Test
    public void testStudentRegistrationAndAuthentication() {
        UserDao userDao = new UserDao();

        long timestamp = System.currentTimeMillis();
        String studentEmail = "student_" + timestamp + "@campus.edu";
        
        User student = new User();
        student.setIdString("user_student_" + timestamp);
        student.setName("Robolectric Student " + timestamp);
        student.setEmail(studentEmail);
        student.setPasswordHash("Pass1234!");
        student.setRole("CUSTOMER"); // Maps to campus_users
        student.setCollegeId(2);
        student.setCollegeIdString("col_2"); // COEP Technological University
        student.setAssignedCanteenId("cant_3"); // Or any canteen
        student.setCampusIdNumber("COEP-" + timestamp);

        boolean inserted = userDao.insertUser(student);
        System.err.println("TEST_DAO_STUDENT_INSERT: " + inserted + " | Err: " + userDao.getLastErrorMessage());
        assertTrue("Student registration should succeed: " + userDao.getLastErrorMessage(), inserted);

        User authenticated = userDao.authenticateUser(studentEmail, "Pass1234!");
        System.err.println("TEST_DAO_STUDENT_AUTH: " + (authenticated != null ? authenticated.getEmail() : "null"));
        assertNotNull("Authenticated user should not be null", authenticated);
    }

    @Test
    public void testSuperAdminRegistrationAndAuthentication() {
        UserDao userDao = new UserDao();

        long timestamp = System.currentTimeMillis();
        String adminEmail = "superadmin_" + timestamp + "@appetite.io";

        User superAdmin = new User();
        superAdmin.setIdString("user_super_" + timestamp);
        superAdmin.setName("Robolectric SuperAdmin " + timestamp);
        superAdmin.setEmail(adminEmail);
        superAdmin.setPasswordHash("SuperAdmin123!");
        superAdmin.setRole("SUPER_ADMIN");
        superAdmin.setCollegeId(null);
        superAdmin.setCollegeIdString(null);

        boolean inserted = userDao.insertUser(superAdmin);
        System.err.println("TEST_DAO_SUPERADMIN_INSERT: " + inserted);
        assertTrue("Super Admin registration should succeed", inserted);

        User authenticated = userDao.authenticateUser(adminEmail, "SuperAdmin123!");
        System.err.println("TEST_DAO_SUPERADMIN_AUTH: " + (authenticated != null ? authenticated.getEmail() : "null"));
        assertNotNull("Authenticated super admin should not be null", authenticated);
    }
}
