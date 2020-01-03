package org.traccar.database;

import org.traccar.model.Customer;

public class CustomersManager extends BaseObjectManager<Customer> {

    public CustomersManager(DataManager dataManager) {
        super(dataManager, Customer.class);
    }
}
