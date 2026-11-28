package org.traccar.model;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PermissionTest {

    @Test
    @DisplayName("Static: User + Device returns tc_user_device")
    public void testStaticGetStorageNameNoPrefixUserDevice() {
        final Class<? extends BaseModel> ownerClass = User.class;
        final Class<? extends BaseModel> propertyClass = Device.class;

        String storageName = Permission.getStorageName(ownerClass, propertyClass);

        assertEquals("tc_user_device", storageName);
    }

    @Test
    @DisplayName("Static: Managed prefix stripped, User + ManagedUser returns tc_user_user")
    public void testStaticGetStorageNameManagedPrefixUserManagedUser() {
        final Class<? extends BaseModel> ownerClass = User.class;
        final Class<? extends BaseModel> propertyClass = ManagedUser.class;

        String storageName = Permission.getStorageName(ownerClass, propertyClass);

        assertEquals("tc_user_user", storageName);
    }

    @Test
    @DisplayName("Static: Linked prefix stripped, Device + LinkedDevice returns tc_device_device")
    public void testStaticGetStorageNameLinkedPrefixDeviceLinkedDevice() {
        final Class<? extends BaseModel> ownerClass = Device.class;
        final Class<? extends BaseModel> propertyClass = LinkedDevice.class;

        String storageName = Permission.getStorageName(ownerClass, propertyClass);

        assertEquals("tc_device_device", storageName);
    }

    @Test
    @DisplayName("Instance: User + Device returns tc_user_device")
    public void testInstanceGetStorageNameUserDevice() {
        final Class<? extends BaseModel> ownerClass = User.class;
        final Class<? extends BaseModel> propertyClass = Device.class;
        final Permission permission = new Permission(ownerClass, 1, propertyClass, 2);

        String storageName = permission.getStorageName();

        assertEquals("tc_user_device", storageName);
    }

    @Test
    @DisplayName("Static: Group + Attribute returns tc_group_attribute")
    public void testStaticGetStorageNameGroupAttribute() {
        final Class<? extends BaseModel> ownerClass = Group.class;
        final Class<? extends BaseModel> propertyClass = Attribute.class;

        String storageName = Permission.getStorageName(ownerClass, propertyClass);

        assertEquals("tc_group_attribute", storageName);
    }

    @Test
    @DisplayName("Static: Group + Geofence returns tc_group_geofence")
    public void testStaticGetStorageNameGroupGeofence() {
        final Class<? extends BaseModel> ownerClass = Group.class;
        final Class<? extends BaseModel> propertyClass = Geofence.class;

        String storageName = Permission.getStorageName(ownerClass, propertyClass);

        assertEquals("tc_group_geofence", storageName);
    }

    @Test
    @DisplayName("Map constructor: userId + deviceId sets owner id 1 and property id 2")
    public void testMapConstructor() {
        final LinkedHashMap<String, Long> data = new LinkedHashMap<>();
        data.put("userId", 1L);
        data.put("deviceId", 2L);

        final Permission permission = new Permission(data);

        assertEquals(1L, permission.getOwnerId());
        assertEquals(2L, permission.getPropertyId());
    }

    @Disabled("ClassScanner scans the test classpath, so class lookup returns null")
    @Test
    @DisplayName("Map constructor: userId + deviceId resolves User and Device classes")
    public void testMapConstructorClassResolution() {
        final LinkedHashMap<String, Long> data = new LinkedHashMap<>();
        data.put("userId", 1L);
        data.put("deviceId", 2L);

        final Permission permission = new Permission(data);

        assertEquals(User.class, permission.getOwnerClass());
        assertEquals(Device.class, permission.getPropertyClass());
    }

    @Test
    @DisplayName("Map constructor: 1 entry (below boundary of 2) throws IllegalArgumentException")
    public void testMapConstructorSizeUnder() {
        final LinkedHashMap<String, Long> data = new LinkedHashMap<>();
        data.put("userId", 1L);

        assertThrows(IllegalArgumentException.class, () -> {Permission permission = new Permission(data);});
    }

    @Test
    @DisplayName("Map constructor: 3 entry (above boundary of 2) throws IllegalArgumentException")
    public void testMapConstructorSizeOver() {
        final LinkedHashMap<String, Long> data = new LinkedHashMap<>();
        data.put("userId", 1L);
        data.put("deviceId", 2L);
        data.put("geofenceId", 3L);

        assertThrows(IllegalArgumentException.class, () -> {Permission permission = new Permission(data);});
    }

    @Test
    @DisplayName("Class constructor: User.class + Device.class sets owner and property")
    public void testClassConstructor() {
        final Class<? extends BaseModel> ownerClass = User.class;
        final Class<? extends BaseModel> propertyClass = Device.class;
        final Permission permission = new Permission(ownerClass, 1, propertyClass, 2);

        assertEquals(User.class, permission.getOwnerClass());
        assertEquals(1L, permission.getOwnerId());
        assertEquals(Device.class, permission.getPropertyClass());
        assertEquals(2L, permission.getPropertyId());
    }
}
