package org.traccar.forward;

import org.junit.jupiter.api.Test;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;

public class EventForwarderMqttTest {

    @Test
    public void testCorrectClientIsCreated() {
        ObjectMapper objectMapperMock = mock(ObjectMapper.class);
        MqttClientFactory clientFactoryMock = mock(MqttClientFactory.class);
        Config configMock = mock(Config.class);
        when(configMock.getString(Keys.EVENT_FORWARD_URL)).thenReturn("mqtt://tester:passwd@localhost:1883");
        when(configMock.getInteger(Keys.EVENT_FORWARD_MQTT_VERSION)).thenReturn(3);
        when(configMock.getString(Keys.EVENT_FORWARD_TOPIC)).thenReturn("testTopic");

        EventForwarderMqtt mqttForwarder = new EventForwarderMqtt(configMock, objectMapperMock, clientFactoryMock);

        verify(clientFactoryMock).create(3, "localhost", 1883, "tester", "passwd", "testTopic");
    }

    @Test
    public void testForwardForwardsCorrectPayload() throws Exception{
        ObjectMapper objectMapperMock = mock(ObjectMapper.class);
        MqttClientFactory clientFactoryMock = mock(MqttClientFactory.class);
        Config configMock = mock(Config.class);
        EventData eventDataMock = mock(EventData.class);
        ResultHandler resultHandlerMock = mock(ResultHandler.class);
        MqttClient clientMock = mock(MqttClient.class);

        when(configMock.getString(Keys.EVENT_FORWARD_URL)).thenReturn("mqtt://tester:passwd@localhost:1883");
        when(configMock.getInteger(Keys.EVENT_FORWARD_MQTT_VERSION)).thenReturn(3);
        when(configMock.getString(Keys.EVENT_FORWARD_TOPIC)).thenReturn("testTopic");
        when(objectMapperMock.writeValueAsString(eventDataMock)).thenReturn("testDataFromObjectMapper");
        when(clientFactoryMock.create(anyInt(), any(), anyInt(), any(), any(), any())).thenReturn(clientMock);

        EventForwarderMqtt mqttForwarder = new EventForwarderMqtt(configMock, objectMapperMock, clientFactoryMock);
        mqttForwarder.forward(eventDataMock, resultHandlerMock);

        verify(clientMock).publish("testDataFromObjectMapper".getBytes(), resultHandlerMock);
    }

}
