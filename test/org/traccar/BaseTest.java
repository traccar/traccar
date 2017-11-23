package org.traccar;

public class BaseTest {
    
    static {
        Context.init(new TestIdentityManager());
    }

}
