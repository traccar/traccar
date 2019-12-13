package org.traccar.database;

import com.google.common.collect.Lists;
import com.zaxxer.hikari.util.FastList;
import org.apache.commons.collections.FastArrayList;
import org.traccar.Context;
import org.traccar.model.Customer;
import org.traccar.model.CustomerLocation;
import org.traccar.model.User;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CustomerLocationManager extends BaseObjectManager<CustomerLocation> {

    public static final String CUSTOMER_ID = "customerid";

    private Map<Long, List<CustomerLocation>> customerIdLocationMap;

    public CustomerLocationManager(DataManager dataManager) {
        super(dataManager, CustomerLocation.class);
        customerIdLocationMap = new ConcurrentHashMap<>();
        refreshCustomerLocations();
    }

    public List<CustomerLocation> getLocationsByCustomerId(long customerId) {
        return customerIdLocationMap.get(customerId);
    }

    public List<CustomerLocation> getLocationByUserId(long userId) throws ParseException {

        UsersManager usersManager = Context.getUsersManager();
        User user = usersManager.getById(userId);

        if (user != null) {
            long customerId = ((Number) user.getInteger(CUSTOMER_ID)).longValue();
            if (customerId != 0) {
                CustomersManager customersManager = Context.getCustomersManager();
                Customer customer = customersManager.getById(customerId);
                if (customer != null) {
                    return customerIdLocationMap.get(customer.getId());
                }
            }
        }
        return null;
    }

    private void refreshCustomerLocations() {
        // TODO: refresh on interval
        Context.getCustomersManager().refreshItems();
        super.refreshItems();
        customerIdLocationMap.clear();

        Set<Long> allItems = getAllItems();
        Collection<CustomerLocation> items = getItems(allItems);

        for (CustomerLocation customerLocation : items) {
            long customerId = customerLocation.getCustomerId();
            if (!customerIdLocationMap.containsKey(customerId)) {

                customerIdLocationMap.put(customerId, Lists.newArrayList());
            }
            List<CustomerLocation> customerLocations = customerIdLocationMap.get(customerId);
            customerLocations.add(customerLocation);
        }
    }
}
