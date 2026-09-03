package com.example.db;

import com.example.model.AppVersionConfig;
import com.example.model.Canteen;
import com.example.model.College;
import com.example.model.MenuItem;
import com.example.model.Order;
import com.example.model.OrderItem;
import com.example.model.User;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory resilient cache and sync layer.
 * Mirrors the Cloud SQL MySQL schema and seeds default data
 * to ensure high availability and responsiveness under any network environment.
 */
public class LocalDataStore {
    private static volatile LocalDataStore instance;

    private final List<College> colleges = new ArrayList<>();
    private final List<Canteen> canteens = new ArrayList<>();
    private final List<User> users = new ArrayList<>();
    private final List<MenuItem> menuItems = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();
    private AppVersionConfig appVersionConfig = new AppVersionConfig(1, "android", 1, "1.0", 1, "1.0", false, "App Update Required", "A new version of AppEtite is available with essential menu availability and ordering updates. Please update to continue.", "https://github.com/StarShree/AppEtiteApp/releases");

    private final AtomicInteger userIdCounter = new AtomicInteger(100);
    private final AtomicInteger orderIdCounter = new AtomicInteger(100);
    private final AtomicInteger menuItemIdCounter = new AtomicInteger(100);
    private final AtomicInteger tokenCounter = new AtomicInteger(300);

    private LocalDataStore() {
        seedInitialData();
    }

    public static LocalDataStore getInstance() {
        if (instance == null) {
            synchronized (LocalDataStore.class) {
                if (instance == null) {
                    instance = new LocalDataStore();
                }
            }
        }
        return instance;
    }

    private synchronized void seedInitialData() {
        // Colleges (3 distinct campus options with short IDs: STAN, IMP, SIT)
        colleges.add(new College("STAN", 1, "Stanford University Campus", "Main Campus, Building A"));
        colleges.add(new College("IMP", 2, "Imperial College of Engineering", "North Campus, Block A"));
        colleges.add(new College("SIT", 3, "St. Jude Institute of Technology", "South Campus, Central Plaza"));

        // 2 Canteens for each of the 3 Colleges (Total 6 Canteens)
        // Stanford University Campus (STAN)
        canteens.add(new Canteen("canteen_1_1", "STAN", 1, "Byte Bites Canteen (Engineering Block)", "Ground Floor, Engineering Wing", "8:00 AM - 9:00 PM"));
        canteens.add(new Canteen("canteen_1_2", "STAN", 1, "The Oval Bistro & Express", "Central Quadrangle, Student Center", "7:30 AM - 10:00 PM"));

        // Imperial College of Engineering (IMP)
        canteens.add(new Canteen("canteen_2_1", "IMP", 2, "Imperial North Food Court", "North Campus, Block A Ground Floor", "8:00 AM - 8:30 PM"));
        canteens.add(new Canteen("canteen_2_2", "IMP", 2, "Royal Roast & Juice Lounge", "East Wing, Science Complex", "8:30 AM - 9:30 PM"));

        // St. Jude Institute of Technology (SIT)
        canteens.add(new Canteen("canteen_3_1", "SIT", 3, "Central Plaza Food Pavilion", "South Campus, Central Plaza Level 1", "7:30 AM - 9:00 PM"));
        canteens.add(new Canteen("canteen_3_2", "SIT", 3, "Tech Hub Cafe & Grille", "Innovation & IT Tower, Level 2", "8:00 AM - 10:30 PM"));

        // Users matching Cloud SQL instance
        User uSuper = new User(1, "Dr. Sarah Vance", "sarah.vance@appetite.io", "Admin@123", "SUPER_ADMIN", null);
        uSuper.setIdString("user_super_1");
        uSuper.setPermissionLevel("ALL");
        users.add(uSuper);

        User uAdmin = new User(2, "Dean Harrison", "dean.harrison@stanford.edu", "Admin@123", "COLLEGE_ADMIN", 1);
        uAdmin.setIdString("user_admin_1");
        uAdmin.setCollegeIdString("STAN");
        uAdmin.setAssignedCanteenId("canteen_1_1");
        uAdmin.setDepartment("Academic Administration & Campus Dining");
        users.add(uAdmin);

        User uVendor = new User(3, "Chef Roberto", "chef.roberto@bytebites.edu", "Kitchen@123", "KITCHEN_STAFF", 1);
        uVendor.setIdString("user_vendor_1");
        uVendor.setCollegeIdString("STAN");
        uVendor.setAssignedCanteenId("canteen_1_1");
        uVendor.setDesignation("Head Chef & Kitchen Lead");
        users.add(uVendor);

        User uStudent = new User(4, "Alex Rivera", "alex.rivera@campus.edu", "Student@123", "CUSTOMER", 1);
        uStudent.setIdString("user_campus_1");
        uStudent.setCollegeIdString("STAN");
        uStudent.setAssignedCanteenId("canteen_1_1");
        uStudent.setStudentRollNumber("2024-CS-042");
        uStudent.setCampusIdNumber("2024-CS-042");
        uStudent.setPhoneNumber("+1 555-0199");
        uStudent.setWalletBalance(new BigDecimal("350.00"));
        uStudent.setStatus("ACTIVE");
        users.add(uStudent);

        // Additional default demo users
        User uSuper2 = new User(5, "Chief System Administrator", "superadmin@appetite.com", "Admin@123", "SUPER_ADMIN", null);
        uSuper2.setPermissionLevel("ALL");
        users.add(uSuper2);

        User uAdmin2 = new User(6, "Prof. Robert Davis", "admin.imperial@appetite.com", "Admin@123", "COLLEGE_ADMIN", 2);
        uAdmin2.setCollegeIdString("IMP");
        uAdmin2.setAssignedCanteenId("canteen_2_1");
        uAdmin2.setDepartment("School of Engineering");
        users.add(uAdmin2);

        User uVendor2 = new User(7, "Chef Mario Rossi", "kitchen.imperial@appetite.com", "Kitchen@123", "KITCHEN_STAFF", 2);
        uVendor2.setCollegeIdString("IMP");
        uVendor2.setAssignedCanteenId("canteen_2_1");
        uVendor2.setDesignation("Executive Chef");
        users.add(uVendor2);

        User uAlex = new User(8, "Alex Johnson", "alex@student.imperial.edu", "Student@123", "CUSTOMER", 2);
        uAlex.setCollegeIdString("IMP");
        uAlex.setAssignedCanteenId("canteen_2_1");
        uAlex.setStudentRollNumber("2023-EE-118");
        uAlex.setCampusIdNumber("2023-EE-118");
        uAlex.setPhoneNumber("+1 555-0144");
        uAlex.setWalletBalance(new BigDecimal("280.00"));
        uAlex.setStatus("ACTIVE");
        users.add(uAlex);

        // St. Jude (SIT) Users
        User uAdmin3 = new User(9, "Dr. Sarah Connor", "admin.stjude@appetite.com", "Admin@123", "COLLEGE_ADMIN", 3);
        uAdmin3.setCollegeIdString("SIT");
        uAdmin3.setAssignedCanteenId("canteen_3_1");
        uAdmin3.setDepartment("Director of Campus Facilities");
        users.add(uAdmin3);

        User uVendor3 = new User(10, "Chef Gordon Patel", "kitchen.stjude@appetite.com", "Kitchen@123", "KITCHEN_STAFF", 3);
        uVendor3.setCollegeIdString("SIT");
        uVendor3.setAssignedCanteenId("canteen_3_1");
        uVendor3.setDesignation("Lead Line Cook & Prep Manager");
        users.add(uVendor3);

        User uEmily = new User(11, "Emily Watson", "emily@student.stjude.edu", "Student@123", "CUSTOMER", 3);
        uEmily.setCollegeIdString("SIT");
        uEmily.setAssignedCanteenId("canteen_3_1");
        uEmily.setStudentRollNumber("2024-ME-077");
        uEmily.setCampusIdNumber("2024-ME-077");
        uEmily.setPhoneNumber("+1 555-0177");
        uEmily.setWalletBalance(new BigDecimal("310.00"));
        uEmily.setStatus("ACTIVE");
        users.add(uEmily);

        // Menu Items for Imperial (College 1)
        menuItems.add(new MenuItem(1, 1, "Avocado & Herb Toast", "Crispy multigrain sourdough with smashed avocado and microgreens", new BigDecimal("85.00"), "Breakfast", "https://images.unsplash.com/photo-1525351484163-7529414344d8?w=500", true));
        menuItems.add(new MenuItem(2, 1, "South Indian Masala Dosa", "Golden fermented rice crepe with spiced potato filling & coconut chutney", new BigDecimal("65.00"), "Breakfast", "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=500", true));
        menuItems.add(new MenuItem(3, 1, "Artisan Margherita Flatbread", "Woodfired sourdough crust, heirloom tomato coulis, fresh mozzarella & basil", new BigDecimal("140.00"), "Lunch", "https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?w=500", true));
        menuItems.add(new MenuItem(4, 1, "Paneer Tikka Rice Bowl", "Smoky tandoori paneer over saffron basmati rice with minted yogurt", new BigDecimal("120.00"), "Lunch", "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500", true));
        menuItems.add(new MenuItem(5, 1, "Cold Brew Caramel Macchiato", "Slow-steeped Arabica coffee with velvet cream & salted caramel", new BigDecimal("70.00"), "Beverages", "https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=500", true));
        menuItems.add(new MenuItem(6, 1, "Matcha Green Tea Cooler", "Ceremonial grade organic matcha shaken with chilled oat milk", new BigDecimal("75.00"), "Beverages", "https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=500", true));
        menuItems.add(new MenuItem(7, 1, "Crispy Sweet Corn Kernels", "Golden sweet corn tossed with freshly cracked black pepper & lime", new BigDecimal("55.00"), "Snacks", "https://images.unsplash.com/photo-1582293041079-7814c2f12063?w=500", true));
        menuItems.add(new MenuItem(8, 1, "Truffle Parmesan Fries", "Hand-cut russet potato batons with white truffle oil & parmesan", new BigDecimal("90.00"), "Snacks", "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=500", true));

        // Menu Items for St. Jude (College 2)
        menuItems.add(new MenuItem(9, 2, "Classic Belgian Waffles", "Golden malted waffles topped with maple syrup & berries", new BigDecimal("95.00"), "Breakfast", "https://images.unsplash.com/photo-1562376552-0d160a2f238d?w=500", true));
        menuItems.add(new MenuItem(10, 2, "Gourmet Veggie Burger", "Herb-roasted portobello & black bean patty with aged cheddar", new BigDecimal("130.00"), "Lunch", "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500", true));
        menuItems.add(new MenuItem(11, 2, "Iced Mango Passion Cooler", "Fresh Alphonso mango puree with passion fruit & mint leaves", new BigDecimal("80.00"), "Beverages", "https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=500", true));
        menuItems.add(new MenuItem(12, 2, "Loaded Cheese Nachos", "Crisp tortilla chips topped with melted jalapeño queso & pico de gallo", new BigDecimal("110.00"), "Snacks", "https://images.unsplash.com/photo-1513456852971-30c0b8199d4d?w=500", true));

        // Menu Items for Stanford (College 3)
        menuItems.add(new MenuItem(13, 3, "Crispy Truffle Chicken Burger", "Panko-crusted chicken fillet with melted Monterey Jack, truffle aioli on toasted brioche", new BigDecimal("6.49"), "Fast Food", "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500", true));
        menuItems.add(new MenuItem(14, 3, "Iced Taro Brown Sugar Boba", "Velvety taro milk tea layered with slow-cooked brown sugar boba pearls", new BigDecimal("4.20"), "Beverages", "https://images.unsplash.com/photo-1558857563-b37cf05d8a58?w=500", true));
        menuItems.add(new MenuItem(15, 3, "Supreme Cheesy Sourdough Pizza", "Hand-tossed crust with mozzarella, smoked provolone, roasted garlic & basil", new BigDecimal("9.69"), "Fast Food", "https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?w=500", true));
        menuItems.add(new MenuItem(16, 3, "Spicy Dan Dan Dragon Noodles", "Wok-tossed noodles with crushed peanuts, scallions, chili crisp & sesame broth", new BigDecimal("7.50"), "Meals", "https://images.unsplash.com/photo-1585032226651-759b368d7246?w=500", true));
        menuItems.add(new MenuItem(17, 3, "Harvest Glow Salmon Poké Bowl", "Wild salmon sashimi, edamame, avocado, sushi rice with citrus ponzu", new BigDecimal("8.50"), "Healthy Bowls", "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500", true));
        menuItems.add(new MenuItem(18, 3, "Handmade Guacamole Veggie Wrap", "Hass avocado, chipotle black beans, sweet corn & crispy romaine in spinach tortilla", new BigDecimal("5.80"), "Veg", "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500", true));
        menuItems.add(new MenuItem(19, 3, "Golden Seasoned Curly Fries", "Spiral-cut potatoes dusted in paprika, garlic herbs & house fry dip", new BigDecimal("3.99"), "Fast Food", "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=500", true));
        menuItems.add(new MenuItem(20, 3, "Nitro Cold Brew Vanilla Foam", "Rich Colombian roast infused with nitrogen & capped with Madagascar vanilla sweet cream", new BigDecimal("4.50"), "Beverages", "https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=500", true));

        // Sample Orders
        Order o1 = new Order(1, 8, 1, new BigDecimal("205.00"), 101, "Placed", new Timestamp(System.currentTimeMillis() - 15 * 60 * 1000));
        o1.setUserName("Alex Rivera");
        o1.setCollegeName("Stanford University Campus");
        o1.setCollegeIdString("STAN");
        o1.setCanteenId("canteen_1_1");
        o1.setCanteenName("Byte Bites Canteen (Engineering Block)");
        List<OrderItem> items1 = new ArrayList<>();
        items1.add(new OrderItem(1, 1, 1, 1, new BigDecimal("85.00")));
        items1.get(0).setItemName("Avocado & Herb Toast");
        items1.add(new OrderItem(2, 1, 4, 1, new BigDecimal("120.00")));
        items1.get(1).setItemName("Paneer Tikka Rice Bowl");
        o1.setItems(items1);
        orders.add(o1);

        Order o2 = new Order(2, 8, 1, new BigDecimal("140.00"), 102, "Preparing", new Timestamp(System.currentTimeMillis() - 9 * 60 * 1000));
        o2.setUserName("Alex Rivera");
        o2.setCollegeName("Stanford University Campus");
        o2.setCollegeIdString("STAN");
        o2.setCanteenId("canteen_1_1");
        o2.setCanteenName("Byte Bites Canteen (Engineering Block)");
        List<OrderItem> items2 = new ArrayList<>();
        items2.add(new OrderItem(3, 2, 3, 1, new BigDecimal("140.00")));
        items2.get(0).setItemName("Artisan Margherita Flatbread");
        o2.setItems(items2);
        orders.add(o2);

        Order o3 = new Order(3, 9, 1, new BigDecimal("175.00"), 103, "Ready", new Timestamp(System.currentTimeMillis() - 4 * 60 * 1000));
        o3.setUserName("Emily Watson");
        o3.setCollegeName("Stanford University Campus");
        o3.setCollegeIdString("STAN");
        o3.setCanteenId("canteen_1_2");
        o3.setCanteenName("The Oval Bistro & Express");
        List<OrderItem> items3 = new ArrayList<>();
        items3.add(new OrderItem(4, 3, 9, 1, new BigDecimal("95.00")));
        items3.get(0).setItemName("Classic Belgian Waffles");
        items3.add(new OrderItem(5, 3, 11, 1, new BigDecimal("80.00")));
        items3.get(1).setItemName("Iced Mango Passion Cooler");
        o3.setItems(items3);
        orders.add(o3);

        Order o4 = new Order(4, 10, 2, new BigDecimal("110.00"), 201, "Placed", new Timestamp(System.currentTimeMillis() - 7 * 60 * 1000));
        o4.setUserName("David Kim");
        o4.setCollegeName("Imperial College of Engineering");
        o4.setCollegeIdString("col_2");
        o4.setCanteenId("canteen_2_1");
        o4.setCanteenName("Imperial North Food Court");
        List<OrderItem> items4 = new ArrayList<>();
        items4.add(new OrderItem(6, 4, 12, 1, new BigDecimal("110.00")));
        items4.get(0).setItemName("Loaded Cheese Nachos");
        o4.setItems(items4);
        orders.add(o4);
    }

    public synchronized User authenticateUser(String email, String password) {
        if (email == null) return null;
        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(email.trim()) && u.getPasswordHash().equals(password)) {
                return u;
            }
        }
        return null;
    }

    public synchronized boolean insertUser(User user) {
        if (user == null) return false;
        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(user.getEmail())) {
                return false; // Email already exists
            }
        }
        if (user.getUserId() == 0) {
            user.setUserId(userIdCounter.incrementAndGet());
        }
        users.add(user);
        return true;
    }

    public synchronized List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public synchronized User getUserById(int userId) {
        for (User u : users) {
            if (u.getUserId() == userId) return u;
        }
        return null;
    }

    public synchronized User getUserById(String idString) {
        if (idString == null) return null;
        for (User u : users) {
            if (idString.equals(u.getIdString())) return u;
        }
        return null;
    }

    public synchronized List<College> getAllColleges() {
        return new ArrayList<>(colleges);
    }

    public synchronized College getCollegeById(int collegeId) {
        for (College c : colleges) {
            if (c.getCollegeId() == collegeId) return c;
        }
        String target = College.toShortCode(null, collegeId, null);
        for (College c : colleges) {
            if (c.getIdString().equalsIgnoreCase(target)) return c;
        }
        return null;
    }

    public synchronized College getCollegeByCode(String code) {
        if (code == null) return null;
        String target = College.toShortCode(code, null, null);
        for (College c : colleges) {
            if (c.getIdString().equalsIgnoreCase(target)) return c;
        }
        return null;
    }

    public synchronized List<Canteen> getAllCanteens() {
        return new ArrayList<>(canteens);
    }

    public synchronized List<Canteen> getCanteensForCollege(String collegeIdString) {
        List<Canteen> result = new ArrayList<>();
        String targetCode = College.toShortCode(collegeIdString, null, null);
        for (Canteen c : canteens) {
            String canteenCol = College.toShortCode(c.getCollegeIdString(), c.getCollegeId(), null);
            if (canteenCol.equalsIgnoreCase(targetCode)) {
                result.add(c);
            }
        }
        return result;
    }

    public synchronized List<MenuItem> getMenuItemsByCollege(int collegeId, String category) {
        List<MenuItem> result = new ArrayList<>();
        for (MenuItem m : menuItems) {
            if (m.getCollegeId() == collegeId) {
                if (category == null || category.equalsIgnoreCase("All") || m.getCategory().equalsIgnoreCase(category)) {
                    result.add(m);
                }
            }
        }
        return result;
    }

    public synchronized boolean insertMenuItem(MenuItem item) {
        if (item == null) return false;
        if (item.getItemId() == 0) {
            item.setItemId(menuItemIdCounter.incrementAndGet());
        }
        menuItems.add(item);
        return true;
    }

    public synchronized boolean updateMenuItemAvailability(int itemId, boolean isAvailable) {
        return updateMenuItemAvailability(null, itemId, isAvailable);
    }

    public synchronized boolean updateMenuItemAvailability(String idString, int itemId, boolean isAvailable) {
        for (MenuItem m : menuItems) {
            boolean match = (itemId > 0 && m.getItemId() == itemId) ||
                    (idString != null && (idString.equalsIgnoreCase(m.getIdString())
                            || idString.equalsIgnoreCase("item_" + m.getItemId())
                            || idString.equalsIgnoreCase(String.format("item_%02d", m.getItemId()))));
            if (match) {
                m.setAvailable(isAvailable);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean deleteMenuItem(int itemId) {
        return menuItems.removeIf(m -> m.getItemId() == itemId);
    }

    public synchronized List<Order> getOrdersByCollege(int collegeId) {
        List<Order> result = new ArrayList<>();
        for (Order o : orders) {
            if (o.getCollegeId() == collegeId) {
                result.add(o);
            }
        }
        Collections.reverse(result);
        return result;
    }

    public synchronized List<Order> getOrdersByCollegeAndStatus(int collegeId, String status) {
        List<Order> result = new ArrayList<>();
        for (Order o : orders) {
            if (o.getCollegeId() == collegeId) {
                if (status == null || status.equalsIgnoreCase("ALL") || o.getOrderStatus().equalsIgnoreCase(status)) {
                    result.add(o);
                }
            }
        }
        Collections.reverse(result);
        return result;
    }

    public synchronized List<Order> getOrdersByCanteenAndStatus(String canteenId, int collegeId, String status) {
        List<Order> result = new ArrayList<>();
        for (Order o : orders) {
            boolean matchesCollege = (collegeId <= 0) || (o.getCollegeId() == collegeId) ||
                    ("col_" + collegeId).equalsIgnoreCase(o.getCollegeIdString());

            boolean matchesCanteen = (canteenId == null || canteenId.trim().isEmpty()) ||
                    (o.getCanteenId() != null && (o.getCanteenId().equalsIgnoreCase(canteenId) ||
                            o.getCanteenId().equalsIgnoreCase(canteenId.replace("canteen_", "cant_")) ||
                            canteenId.equalsIgnoreCase(o.getCanteenId().replace("canteen_", "cant_")))) ||
                    (o.getCanteenName() != null && o.getCanteenName().equalsIgnoreCase(canteenId));

            if (matchesCollege && matchesCanteen) {
                if (status == null || status.equalsIgnoreCase("ALL") || o.getOrderStatus().equalsIgnoreCase(status)) {
                    result.add(o);
                }
            }
        }
        Collections.reverse(result);
        return result;
    }

    public synchronized List<Order> getCustomerOrders(int userId) {
        return getCustomerOrders(userId, null, null);
    }

    public synchronized List<Order> getCustomerOrders(int userId, String userIdStr, String userName) {
        List<Order> result = new ArrayList<>();
        for (Order o : orders) {
            boolean match = (userId > 0 && o.getUserId() == userId) ||
                    (userIdStr != null && (userIdStr.equalsIgnoreCase(o.getUserIdString()) || userIdStr.equalsIgnoreCase("user_" + o.getUserId()))) ||
                    (userName != null && o.getUserName() != null && userName.equalsIgnoreCase(o.getUserName()));
            if (match) {
                result.add(o);
            }
        }
        Collections.reverse(result);
        return result;
    }

    public synchronized List<Order> getAllOrders() {
        List<Order> copy = new ArrayList<>(orders);
        Collections.reverse(copy);
        return copy;
    }

    public synchronized void createOrUpdateOrder(Order order) {
        if (order == null) return;
        for (int i = 0; i < orders.size(); i++) {
            Order existing = orders.get(i);
            boolean match = (existing.getOrderId() > 0 && existing.getOrderId() == order.getOrderId()) ||
                    (existing.getIdString() != null && existing.getIdString().equalsIgnoreCase(order.getIdString())) ||
                    (existing.getOrderNumber() != null && existing.getOrderNumber().equalsIgnoreCase(order.getOrderNumber())) ||
                    (existing.getTokenString() != null && existing.getTokenString().equalsIgnoreCase(order.getTokenString()));
            if (match) {
                orders.set(i, order);
                return;
            }
        }
        orders.add(order);
    }

    public synchronized boolean updateOrderStatus(int orderId, String newStatus) {
        return updateOrderStatus(null, "ORD-" + orderId, "T-" + orderId, newStatus) ||
                updateOrderStatus(null, String.valueOf(orderId), null, newStatus);
    }

    public synchronized boolean updateOrderStatus(String idString, String orderNumber, String tokenNumber, String newStatus) {
        for (Order o : orders) {
            boolean match = (idString != null && !idString.isEmpty() && idString.equalsIgnoreCase(o.getIdString())) ||
                    (orderNumber != null && !orderNumber.isEmpty() && (orderNumber.equalsIgnoreCase(o.getOrderNumber()) || orderNumber.replaceAll("\\D+", "").equals(String.valueOf(o.getOrderId())))) ||
                    (tokenNumber != null && !tokenNumber.isEmpty() && tokenNumber.equalsIgnoreCase(o.getTokenString()));
            if (match) {
                o.setOrderStatus(newStatus);
                return true;
            }
        }
        return false;
    }

    public synchronized int createOrder(Order order, List<OrderItem> items) {
        if (order == null) return -1;
        int newOrderId = orderIdCounter.incrementAndGet();
        order.setOrderId(newOrderId);
        int token = tokenCounter.incrementAndGet();
        order.setTokenNumber(token);
        order.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        // Resolve names
        College c = getCollegeById(order.getCollegeId());
        if (c != null) order.setCollegeName(c.getName());
        for (User u : users) {
            if (u.getUserId() == order.getUserId()) {
                order.setUserName(u.getName());
                break;
            }
        }

        if (items != null) {
            for (OrderItem item : items) {
                item.setOrderId(newOrderId);
                for (MenuItem m : menuItems) {
                    if (m.getItemId() == item.getItemId()) {
                        item.setItemName(m.getName());
                        item.setItemCategory(m.getCategory());
                        break;
                    }
                }
            }
            order.setItems(new ArrayList<>(items));
        }

        orders.add(order);
        return newOrderId;
    }

    public synchronized AppVersionConfig getAppVersionConfig(String platform) {
        if (appVersionConfig == null) {
            appVersionConfig = new AppVersionConfig(1, "android", 1, "1.0", 1, "1.0", false, "App Update Required", "A new version of AppEtite is available with essential menu availability and ordering updates. Please update to continue.", "https://github.com/StarShree/AppEtiteApp/releases");
        }
        return appVersionConfig;
    }

    public synchronized boolean updateAppVersionConfig(AppVersionConfig config) {
        if (config != null) {
            this.appVersionConfig = config;
            return true;
        }
        return false;
    }
}
