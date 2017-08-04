package org.traccar.database;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.model.Attribute;
import org.traccar.model.Device;
import org.traccar.model.Driver;
import org.traccar.model.Geofence;
import org.traccar.model.Group;
import org.traccar.model.ManagedUser;
import org.traccar.model.Position;
import org.traccar.model.User;

public class DataManagerTest {

    @Test
    public void constructObjectQuery() {
        Assert.assertEquals("SELECT * FROM users",
                DataManager.constructObjectQuery(DataManager.ACTION_SELECT_ALL, User.class, false));
        Assert.assertEquals("DELETE FROM groups WHERE id = :id",
                DataManager.constructObjectQuery(DataManager.ACTION_DELETE, Group.class, false));
        Assert.assertEquals("SELECT * FROM positions WHERE id = :id",
                DataManager.constructObjectQuery(DataManager.ACTION_SELECT, Position.class, false));

        String insertDevice = DataManager.constructObjectQuery(DataManager.ACTION_INSERT, Device.class, false);
        Assert.assertFalse(insertDevice.contains("class"));
        Assert.assertFalse(insertDevice.contains("id"));
        Assert.assertFalse(insertDevice.contains("status"));
        Assert.assertFalse(insertDevice.contains("geofenceIds"));

        String updateDeviceStatus = DataManager.constructObjectQuery("update", Device.class, true);
        Assert.assertTrue(updateDeviceStatus.contains("lastUpdate"));

        String updateUser = DataManager.constructObjectQuery(DataManager.ACTION_UPDATE, User.class, false);
        Assert.assertFalse(updateUser.contains("class"));
        Assert.assertFalse(updateUser.contains("password"));
        Assert.assertFalse(updateUser.contains("salt"));

        String updateUserPassword = DataManager.constructObjectQuery(DataManager.ACTION_UPDATE, User.class, true);
        Assert.assertFalse(updateUserPassword.contains("name"));
        Assert.assertTrue(updateUserPassword.contains("hashedPassword"));
        Assert.assertTrue(updateUserPassword.contains("salt"));

        String insertPosition = DataManager.constructObjectQuery(DataManager.ACTION_INSERT, Position.class, false);
        Assert.assertFalse(insertPosition.contains("type"));
        Assert.assertFalse(insertPosition.contains("outdated"));

    }

    @Test
    public void constructPermissionsQuery() {
        Assert.assertEquals("SELECT userId, deviceId FROM user_device",
                DataManager.constructPermissionQuery(DataManager.ACTION_SELECT_ALL, User.class, Device.class));

        Assert.assertEquals("SELECT userId, managedUserId FROM user_user",
                DataManager.constructPermissionQuery(DataManager.ACTION_SELECT_ALL, User.class, ManagedUser.class));

        Assert.assertEquals("SELECT deviceId, driverId FROM device_driver",
                DataManager.constructPermissionQuery(DataManager.ACTION_SELECT_ALL, Device.class, Driver.class));

        Assert.assertEquals("SELECT groupId, geofenceId FROM group_geofence",
                DataManager.constructPermissionQuery(DataManager.ACTION_SELECT_ALL, Group.class, Geofence.class));

        Assert.assertEquals("INSERT INTO user_device (userId, deviceId) VALUES (:userId, :deviceId)",
                DataManager.constructPermissionQuery(DataManager.ACTION_INSERT, User.class, Device.class));

        Assert.assertEquals("DELETE FROM user_user WHERE userId = :userId AND managedUserId = :managedUserId",
                DataManager.constructPermissionQuery(DataManager.ACTION_DELETE, User.class, ManagedUser.class));

        Assert.assertEquals("INSERT INTO device_geofence (deviceId, geofenceId) VALUES (:deviceId, :geofenceId)",
                DataManager.constructPermissionQuery(DataManager.ACTION_INSERT, Device.class, Geofence.class));

        Assert.assertEquals("DELETE FROM group_attribute WHERE groupId = :groupId AND attributeId = :attributeId",
                DataManager.constructPermissionQuery(DataManager.ACTION_DELETE, Group.class, Attribute.class));

    }

}
