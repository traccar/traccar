package org.traccar.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traccar.config.Config;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueryBuilderTest {

    private DataSource dataSource;
    private Config config;
    private ObjectMapper objectMapper;

    public static class TestEntity {
        private long id;
        private boolean active;
        private int count;
        private long deviceId;
        private double speed;
        private String name;
        private Date fixTime;
        private byte[] data;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public boolean getActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public long getDeviceId() { return deviceId; }
        public void setDeviceId(long deviceId) { this.deviceId = deviceId; }
        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Date getFixTime() { return fixTime; }
        public void setFixTime(Date fixTime) { this.fixTime = fixTime; }
        public byte[] getData() { return data; }
        public void setData(byte[] data) { this.data = data; }
    }

    @BeforeEach
    public void setUp() throws Exception {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:querybuildertest;DB_CLOSE_DELAY=-1");
        dataSource = h2;
        config = new Config();
        objectMapper = new ObjectMapper();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS test_entity");
            statement.execute(
                    "CREATE TABLE test_entity ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "active BOOLEAN,"
                            + "count INTEGER,"
                            + "deviceId BIGINT,"
                            + "speed DOUBLE,"
                            + "name VARCHAR(255),"
                            + "fixTime TIMESTAMP,"
                            + "data VARBINARY(255))");
        }
    }

    @Test
    public void roundTripsAllPrimitiveAndReferenceTypes() throws Exception {
        Date now = new Date(1700000000000L);
        byte[] payload = {1, 2, 3, 4, 5};

        try (QueryBuilder insert = QueryBuilder.create(config, dataSource, objectMapper,
                "INSERT INTO test_entity(active, count, deviceId, speed, name, fixTime, data) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)", true)) {
            insert.setBoolean(0, true);
            insert.setInteger(1, 42);
            insert.setLong(2, 7L);
            insert.setDouble(3, 99.5);
            insert.setString(4, "hello");
            insert.setDate(5, now);
            insert.setBlob(6, payload);
            long id = insert.executeUpdate();
            assertTrue(id > 0);
        }

        try (QueryBuilder query = QueryBuilder.create(config, dataSource, objectMapper,
                "SELECT * FROM test_entity");
             Stream<TestEntity> stream = query.executeQueryStreamed(TestEntity.class)) {
            List<TestEntity> results = stream.toList();
            assertEquals(1, results.size());
            TestEntity entity = results.get(0);
            assertEquals(true, entity.getActive());
            assertEquals(42, entity.getCount());
            assertEquals(7L, entity.getDeviceId());
            assertEquals(99.5, entity.getSpeed(), 0.0);
            assertEquals("hello", entity.getName());
            assertEquals(now, entity.getFixTime());
            assertArrayEquals(payload, entity.getData());
        }
    }

    @Test
    public void setObjectInsertAndReadBack() throws Exception {
        Date now = new Date(1700000000000L);
        TestEntity entity = new TestEntity();
        entity.setActive(false);
        entity.setCount(7);
        entity.setDeviceId(123L);
        entity.setSpeed(12.5);
        entity.setName("world");
        entity.setFixTime(now);
        entity.setData(new byte[] {9, 8, 7});

        List<String> columns = List.of("active", "count", "deviceId", "speed", "name", "fixTime", "data");
        try (QueryBuilder insert = QueryBuilder.create(config, dataSource, objectMapper,
                "INSERT INTO test_entity(active, count, deviceId, speed, name, fixTime, data) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)", true)) {
            insert.setObject(entity, columns);
            insert.executeUpdate();
        }

        try (QueryBuilder query = QueryBuilder.create(config, dataSource, objectMapper,
                "SELECT * FROM test_entity");
             Stream<TestEntity> stream = query.executeQueryStreamed(TestEntity.class)) {
            List<TestEntity> results = stream.toList();
            assertEquals(1, results.size());
            TestEntity loaded = results.get(0);
            assertEquals(false, loaded.getActive());
            assertEquals(7, loaded.getCount());
            assertEquals(123L, loaded.getDeviceId());
            assertEquals(12.5, loaded.getSpeed(), 0.0);
            assertEquals("world", loaded.getName());
            assertEquals(now, loaded.getFixTime());
            assertArrayEquals(new byte[] {9, 8, 7}, loaded.getData());
        }
    }

    @Test
    public void executeBatchInsertsMultipleRows() throws Exception {
        try (QueryBuilder insert = QueryBuilder.create(config, dataSource, objectMapper,
                "INSERT INTO test_entity(name, count) VALUES (?, ?)", true)) {
            for (int i = 0; i < 3; i++) {
                insert.setString(0, "row" + i);
                insert.setInteger(1, i);
                insert.addBatch();
            }
            List<Long> ids = insert.executeBatch();
            assertEquals(3, ids.size());
        }

        try (QueryBuilder query = QueryBuilder.create(config, dataSource, objectMapper,
                "SELECT * FROM test_entity ORDER BY count");
             Stream<TestEntity> stream = query.executeQueryStreamed(TestEntity.class)) {
            List<TestEntity> results = stream.toList();
            assertEquals(3, results.size());
            assertEquals("row0", results.get(0).getName());
            assertEquals("row2", results.get(2).getName());
        }
    }

}
