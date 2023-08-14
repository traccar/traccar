package org.traccar.forward;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MqttClientFactoryTest {

    @Test
    public void testVersion3CreatesVersion3Client() {
        MqttClientFactory factoryToTest = new MqttClientFactory();
        
        MqttClient client = factoryToTest.create(3, "host", 1883, "user", "password", "topic");
        assertTrue( client instanceof MqttClientV3); 
    }

    @Test
    public void testVersion5CreatesVersion5Client() {
        MqttClientFactory factoryToTest = new MqttClientFactory();
        
        MqttClient client = factoryToTest.create(5, "host", 1883, "user", "password", "topic");
        assertTrue( client instanceof MqttClientV5); 
    }

}
