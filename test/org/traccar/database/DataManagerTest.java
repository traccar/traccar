package org.traccar.database;

import org.junit.Test;
import org.traccar.model.Attribute;
import org.traccar.model.Device;
import org.traccar.model.Driver;
import org.traccar.model.Geofence;
import org.traccar.model.Group;
import org.traccar.model.ManagedUser;
import org.traccar.model.Position;
import org.traccar.model.User;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DataManagerTest {

    @Test
    public void constructObjectQuery() {
        assertEquals("SELECT * FROM tc_users",
                DataManager.constructObjectQuery(DataManager.ACTION_SELECT_ALL, User.class, false));
        assertEquals("DELETE FROM tc_groups WHERE id = :id",
                DataManager.constructObjectQuery(DataManager.ACTION_DELETE, Group.class, false));
        assertEquals("SELECT * FROM tc_positions WHERE id = :id",
                DataManager.constructObjectQuery(DataManager.ACTION_SELECT, Position.class, false));

        String insertDevice = DataManager.constructObjectQuery(DataManager.ACTION_INSERT, Device.class, false);
        assertFalse(insertDevice.contains("class"));
        assertFalse(insertDevice.contains("id"));
        assertFalse(insertDevice.contains("status"));
        assertFalse(insertDevice.contains("geofenceIds"));

        String updateDeviceStatus = DataManager.constructObjectQuery("update", Device.class, true);
        assertTrue(updateDeviceStatus.contains("lastUpdate"));

        String updateUser = DataManager.constructObjectQuery(DataManager.ACTION_UPDATE, User.class, false);
        assertFalse(updateUser.contains("class"));
        assertFalse(updateUser.contains("password"));
        assertFalse(updateUser.contains("salt"));

        String updateUserPassword = DataManager.constructObjectQuery(DataManager.ACTION_UPDATE, User.class, true);
        assertFalse(updateUserPassword.contains("name"));
        assertTrue(updateUserPassword.contains("hashedPassword"));
        assertTrue(updateUserPassword.contains("salt"));

        String insertPosition = DataManager.constructObjectQuery(DataManager.ACTION_INSERT, Position.class, false);
        assertFalse(insertPosition.contains("type"));
        assertFalse(insertPosition.contains("outdated"));

    }

    @Test
    public void constructPermissionsQuery() {
        assertEquals("SELECT userId, deviceId FROM tc_user_device",
                DataManager.constructPermissionQuery(DataManager.ACTION_SELECT_ALL, User.class, Device.class));

        assertEquals("SELECT userId, managedUserId FROM tc_user_user",
                DataManager.constructPermissionQuery(DataManager.ACTION_SELECT_ALL, User.class, ManagedUser.class));

        assertEquals("SELECT deviceId, driverId FROM tc_device_driver",
                DataManager.constructPermissionQuery(DataManager.ACTION_SELECT_ALL, Device.class, Driver.class));

        assertEquals("SELECT groupId, geofenceId FROM tc_group_geofence",
                DataManager.constructPermissionQuery(DataManager.ACTION_SELECT_ALL, Group.class, Geofence.class));

        assertEquals("INSERT INTO tc_user_device (userId, deviceId) VALUES (:userId, :deviceId)",
                DataManager.constructPermissionQuery(DataManager.ACTION_INSERT, User.class, Device.class));

        assertEquals("DELETE FROM tc_user_user WHERE userId = :userId AND managedUserId = :managedUserId",
                DataManager.constructPermissionQuery(DataManager.ACTION_DELETE, User.class, ManagedUser.class));

        assertEquals("INSERT INTO tc_device_geofence (deviceId, geofenceId) VALUES (:deviceId, :geofenceId)",
                DataManager.constructPermissionQuery(DataManager.ACTION_INSERT, Device.class, Geofence.class));

        assertEquals("DELETE FROM tc_group_attribute WHERE groupId = :groupId AND attributeId = :attributeId",
                DataManager.constructPermissionQuery(DataManager.ACTION_DELETE, Group.class, Attribute.class));

    }

}
