package com.example;

import com.example.db.AppVersionDao;
import com.example.db.LocalDataStore;
import com.example.db.MenuItemDao;
import com.example.model.AppVersionConfig;
import com.example.model.MenuItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AppVersionAndAvailabilityTest {

    @Before
    public void setUp() {
        // Reset or seed local data store
        LocalDataStore.getInstance();
    }

    @Test
    public void testAppVersionConfigLogic() {
        AppVersionConfig config = new AppVersionConfig(
                1, "android", 2, "1.1", 3, "1.2", false,
                "Mandatory Update", "Please update your app.", "https://example.com/update"
        );

        // App version 1 is below min_version_code 2 -> Mandatory update required
        assertTrue("Version 1 should require update when min is 2", config.isUpdateRequired(1));

        // App version 2 equals min_version_code 2, force update is false -> Allowed
        assertFalse("Version 2 should NOT require update when min is 2 and force is false", config.isUpdateRequired(2));

        // Version 2 is below latest 3 -> Recommended update
        assertTrue("Version 2 should recommend update when latest is 3", config.isUpdateRecommended(2));

        // App version 3 equals latest -> Neither required nor recommended
        assertFalse(config.isUpdateRequired(3));
        assertFalse(config.isUpdateRecommended(3));

        // When force update is true: version 2 is below latest 3 -> Required
        config.setForceUpdate(true);
        assertTrue("Version 2 should require update when forceUpdate is true and latest is 3", config.isUpdateRequired(2));
    }

    @Test
    public void testAppVersionDaoLocalFallback() {
        AppVersionDao dao = new AppVersionDao();
        AppVersionConfig config = dao.getVersionConfig("android");

        assertNotNull("Version config should not be null", config);
        assertEquals("android", config.getPlatform());
        assertTrue("Min version code should be at least 1", config.getMinVersionCode() >= 1);

        // Test updating version config
        config.setMinVersionCode(5);
        config.setMinVersionName("2.0");
        boolean updated = dao.updateVersionConfig(config);
        assertTrue("Update version config should succeed", updated);

        AppVersionConfig refetched = dao.getVersionConfig("android");
        assertEquals(5, refetched.getMinVersionCode());
        assertEquals("2.0", refetched.getMinVersionName());

        // Restore default
        refetched.setMinVersionCode(1);
        refetched.setMinVersionName("1.0");
        dao.updateVersionConfig(refetched);
    }

    @Test
    public void testMenuItemAvailabilityToggle() {
        MenuItemDao dao = new MenuItemDao();
        List<MenuItem> items = dao.getMenuItemsByCollege(1, "All");
        assertNotNull("Items should not be null", items);
        assertFalse("Items should not be empty", items.isEmpty());

        MenuItem target = items.get(0);
        int targetId = target.getItemId();

        // 1. Mark as Unavailable / Sold Out
        boolean okUnavailable = dao.updateMenuItemAvailability(targetId, false);
        assertTrue("Updating item to unavailable should succeed", okUnavailable);

        List<MenuItem> refreshed = dao.getMenuItemsByCollege(1, "All");
        MenuItem updatedItem = null;
        for (MenuItem m : refreshed) {
            if (m.getItemId() == targetId) {
                updatedItem = m;
                break;
            }
        }
        assertNotNull("Target item should be found", updatedItem);
        assertFalse("Item should be marked unavailable", updatedItem.isAvailable());

        // 2. Mark back to Available
        boolean okAvailable = dao.updateMenuItemAvailability(targetId, true);
        assertTrue("Updating item to available should succeed", okAvailable);

        List<MenuItem> restoredList = dao.getMenuItemsByCollege(1, "All");
        MenuItem restoredItem = null;
        for (MenuItem m : restoredList) {
            if (m.getItemId() == targetId) {
                restoredItem = m;
                break;
            }
        }
        assertNotNull("Restored item should be found", restoredItem);
        assertTrue("Item should be marked available", restoredItem.isAvailable());
    }

    @Test
    public void testCloudSqlOrdersRetrievalAndStatusUpdate() {
        com.example.db.OrderDao orderDao = new com.example.db.OrderDao();

        // 1. Test Customer Orders with different user ID formats
        List<com.example.model.Order> custOrdersByStr = orderDao.getCustomerOrders(1, "user_1", "Alex Rivera");
        org.junit.Assert.assertNotNull(custOrdersByStr);
        org.junit.Assert.assertTrue("Customer orders should have at least 6 orders", custOrdersByStr.size() >= 6);

        List<com.example.model.Order> custOrdersByCampus = orderDao.getCustomerOrders(1, "user_campus_1", "Alex Rivera");
        org.junit.Assert.assertNotNull(custOrdersByCampus);
        org.junit.Assert.assertTrue("Customer orders should match user_campus_1", custOrdersByCampus.size() >= 6);

        // 2. Test Kitchen Orders with both cant_1 and canteen_1_1
        List<com.example.model.Order> kitchenOrders1 = orderDao.getOrdersByCanteen("cant_1", 1);
        org.junit.Assert.assertNotNull(kitchenOrders1);
        org.junit.Assert.assertTrue("Kitchen orders for cant_1 should return orders", kitchenOrders1.size() >= 6);

        List<com.example.model.Order> kitchenOrders2 = orderDao.getOrdersByCanteen("canteen_1_1", 1);
        org.junit.Assert.assertNotNull(kitchenOrders2);
        org.junit.Assert.assertTrue("Kitchen orders for canteen_1_1 should resolve to cant_1 orders", kitchenOrders2.size() >= 6);

        // 3. Test Order Status Update
        boolean updated = orderDao.updateOrderStatus(null, "ORD-4081", "T-230", "PREPARING");
        org.junit.Assert.assertTrue("Status update to PREPARING should succeed", updated);

        List<com.example.model.Order> verifyOrders = orderDao.getCustomerOrders(1, "user_campus_1", "Alex Rivera");
        boolean foundPreparing = false;
        for (com.example.model.Order o : verifyOrders) {
            if ("ORD-4081".equals(o.getOrderNumber())) {
                org.junit.Assert.assertEquals("PREPARING", o.getOrderStatus());
                foundPreparing = true;
                break;
            }
        }
        org.junit.Assert.assertTrue("Order ORD-4081 should be PREPARING", foundPreparing);

        // 4. Restore status to PLACED
        boolean restored = orderDao.updateOrderStatus(null, "ORD-4081", "T-230", "PLACED");
        org.junit.Assert.assertTrue("Status restore to PLACED should succeed", restored);
    }
}
