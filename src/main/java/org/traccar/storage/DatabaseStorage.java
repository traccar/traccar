/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.traccar.config.Config;
import org.traccar.model.BaseModel;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.GroupedModel;
import org.traccar.model.Permission;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Limit;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DatabaseStorage extends Storage {

    private final Config config;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    @Inject
    public DatabaseStorage(Config config, DataSource dataSource, ObjectMapper objectMapper) {
        this.config = config;
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> List<T> getObjects(Class<T> clazz, Request request) throws StorageException {
        StringBuilder query = new StringBuilder("SELECT ");
        if (request.getColumns() instanceof Columns.All) {
            query.append('*');
        } else {
            query.append(formatColumns(request.getColumns(), clazz, "get", c -> c));
        }
        query.append(" FROM ").append(getStorageName(clazz));
        query.append(formatCondition(request.getCondition()));
        query.append(formatOrder(request.getOrder()));
        query.append(formatLimit(request.getLimit()));
        try {
            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query.toString());
            for (Map.Entry<String, Object> variable : getConditionVariables(request.getCondition()).entrySet()) {
                builder.setValue(variable.getKey(), variable.getValue());
            }
            return builder.executeQuery(clazz);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public <T> long addObject(T entity, Request request) throws StorageException {
        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(getStorageName(entity.getClass()));
        query.append("(");
        query.append(formatColumns(request.getColumns(), entity.getClass(), "set", c -> c));
        query.append(") VALUES (");
        query.append(formatColumns(request.getColumns(), entity.getClass(), "set", c -> ':' + c));
        query.append(")");
        try {
            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query.toString(), true);
            builder.setObject(entity);
            return builder.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public <T> void updateObject(T entity, Request request) throws StorageException {
        StringBuilder query = new StringBuilder("UPDATE ");
        query.append(getStorageName(entity.getClass()));
        query.append(" SET ");
        query.append(formatColumns(request.getColumns(), entity.getClass(), "set", c -> c + " = :" + c));
        query.append(formatCondition(request.getCondition()));
        try {
            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query.toString());
            builder.setObject(entity);
            for (Map.Entry<String, Object> variable : getConditionVariables(request.getCondition()).entrySet()) {
                builder.setValue(variable.getKey(), variable.getValue());
            }
            builder.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void removeObject(Class<?> clazz, Request request) throws StorageException {
        StringBuilder query = new StringBuilder("DELETE FROM ");
        query.append(getStorageName(clazz));
        query.append(formatCondition(request.getCondition()));
        try {
            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query.toString());
            for (Map.Entry<String, Object> variable : getConditionVariables(request.getCondition()).entrySet()) {
                builder.setValue(variable.getKey(), variable.getValue());
            }
            builder.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Permission> getPermissions(
            Class<? extends BaseModel> ownerClass, long ownerId,
            Class<? extends BaseModel> propertyClass, long propertyId) throws StorageException {
        StringBuilder query = new StringBuilder("SELECT * FROM ");
        query.append(Permission.getStorageName(ownerClass, propertyClass));
        var conditions = new LinkedList<Condition>();
        if (ownerId > 0) {
            conditions.add(new Condition.Equals(
                    Permission.getKey(ownerClass), Permission.getKey(ownerClass), ownerId));
        }
        if (propertyId > 0) {
            conditions.add(new Condition.Equals(
                    Permission.getKey(propertyClass), Permission.getKey(propertyClass), propertyId));
        }
        Condition combinedCondition = Condition.merge(conditions);
        query.append(formatCondition(combinedCondition));
        try {
            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query.toString());
            for (Map.Entry<String, Object> variable : getConditionVariables(combinedCondition).entrySet()) {
                builder.setValue(variable.getKey(), variable.getValue());
            }
            return builder.executePermissionsQuery();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void addPermission(Permission permission) throws StorageException {
        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(permission.getStorageName());
        query.append(" VALUES (");
        query.append(permission.get().keySet().stream().map(key -> ':' + key).collect(Collectors.joining(", ")));
        query.append(")");
        try {
            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query.toString(), true);
            for (var entry : permission.get().entrySet()) {
                builder.setLong(entry.getKey(), entry.getValue());
            }
            builder.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void removePermission(Permission permission) throws StorageException {
        StringBuilder query = new StringBuilder("DELETE FROM ");
        query.append(permission.getStorageName());
        query.append(" WHERE ");
        query.append(permission
                .get().keySet().stream().map(key -> key + " = :" + key).collect(Collectors.joining(" AND ")));
        try {
            QueryBuilder builder = QueryBuilder.create(config, dataSource, objectMapper, query.toString(), true);
            for (var entry : permission.get().entrySet()) {
                builder.setLong(entry.getKey(), entry.getValue());
            }
            builder.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    private String getStorageName(Class<?> clazz) throws StorageException {
        StorageName storageName = clazz.getAnnotation(StorageName.class);
        if (storageName == null) {
            throw new StorageException("StorageName annotation is missing");
        }
        return storageName.value();
    }

    private Map<String, Object> getConditionVariables(Condition genericCondition) {
        Map<String, Object> results = new HashMap<>();
        if (genericCondition instanceof Condition.Compare) {
            var condition = (Condition.Compare) genericCondition;
            if (condition.getValue() != null) {
                results.put(condition.getVariable(), condition.getValue());
            }
        } else if (genericCondition instanceof Condition.Between) {
            var condition = (Condition.Between) genericCondition;
            results.put(condition.getFromVariable(), condition.getFromValue());
            results.put(condition.getToVariable(), condition.getToValue());
        } else if (genericCondition instanceof Condition.Binary) {
            var condition = (Condition.Binary) genericCondition;
            results.putAll(getConditionVariables(condition.getFirst()));
            results.putAll(getConditionVariables(condition.getSecond()));
        } else if (genericCondition instanceof Condition.Permission) {
            var condition = (Condition.Permission) genericCondition;
            if (condition.getOwnerId() > 0) {
                results.put(Permission.getKey(condition.getOwnerClass()), condition.getOwnerId());
            } else {
                results.put(Permission.getKey(condition.getPropertyClass()), condition.getPropertyId());
            }
        } else if (genericCondition instanceof Condition.LatestPositions) {
            var condition = (Condition.LatestPositions) genericCondition;
            if (condition.getDeviceId() > 0) {
                results.put("deviceId", condition.getDeviceId());
            }
        }
        return results;
    }

    private String formatColumns(
            Columns columns, Class<?> clazz, String type, Function<String, String> mapper) {
        return columns.getColumns(clazz, type).stream().map(mapper).collect(Collectors.joining(", "));
    }

    private String formatCondition(Condition genericCondition) throws StorageException {
        return formatCondition(genericCondition, true);
    }

    private String formatCondition(Condition genericCondition, boolean appendWhere) throws StorageException {
        StringBuilder result = new StringBuilder();
        if (genericCondition != null) {
            if (appendWhere) {
                result.append(" WHERE ");
            }
            if (genericCondition instanceof Condition.Compare) {

                var condition = (Condition.Compare) genericCondition;
                result.append(condition.getColumn());
                result.append(" ");
                result.append(condition.getOperator());
                result.append(" :");
                result.append(condition.getVariable());

            } else if (genericCondition instanceof Condition.Between) {

                var condition = (Condition.Between) genericCondition;
                result.append(condition.getColumn());
                result.append(" BETWEEN :");
                result.append(condition.getFromVariable());
                result.append(" AND :");
                result.append(condition.getToVariable());

            } else if (genericCondition instanceof Condition.Binary) {

                var condition = (Condition.Binary) genericCondition;
                result.append(formatCondition(condition.getFirst(), false));
                result.append(" ");
                result.append(condition.getOperator());
                result.append(" ");
                result.append(formatCondition(condition.getSecond(), false));

            } else if (genericCondition instanceof Condition.Permission) {

                var condition = (Condition.Permission) genericCondition;
                result.append("id IN (");
                result.append(formatPermissionQuery(condition));
                result.append(")");

            } else if (genericCondition instanceof Condition.LatestPositions) {

                var condition = (Condition.LatestPositions) genericCondition;
                result.append("id IN (");
                result.append("SELECT positionId FROM ");
                result.append(getStorageName(Device.class));
                if (condition.getDeviceId() > 0) {
                    result.append(" WHERE id = :deviceId");
                }
                result.append(")");

            }
        }
        return result.toString();
    }

    private String formatOrder(Order order) {
        StringBuilder result = new StringBuilder();
        if (order != null) {
            result.append(" ORDER BY ");
            result.append(order.getColumn());
            if (order.getDescending()) {
                result.append(" DESC");
            }
        }
        return result.toString();
    }

    private String formatLimit(Limit limit) {
        StringBuilder result = new StringBuilder();
        if (limit != null) {
            result.append(" LIMIT ");
            result.append(limit.getValue());
        }
        return result.toString();
    }

    private String formatPermissionQuery(Condition.Permission condition) throws StorageException {
        StringBuilder result = new StringBuilder();

        String outputKey;
        String conditionKey;
        if (condition.getOwnerId() > 0) {
            outputKey = Permission.getKey(condition.getPropertyClass());
            conditionKey = Permission.getKey(condition.getOwnerClass());
        } else {
            outputKey = Permission.getKey(condition.getOwnerClass());
            conditionKey = Permission.getKey(condition.getPropertyClass());
        }

        String storageName = Permission.getStorageName(condition.getOwnerClass(), condition.getPropertyClass());
        result.append("SELECT ");
        result.append(storageName).append('.').append(outputKey);
        result.append(" FROM ");
        result.append(storageName);
        result.append(" WHERE ");
        result.append(conditionKey);
        result.append(" = :");
        result.append(conditionKey);

        if (condition.getIncludeGroups()) {

            boolean expandDevices;
            String groupStorageName;
            if (GroupedModel.class.isAssignableFrom(condition.getOwnerClass())) {
                expandDevices = Device.class.isAssignableFrom(condition.getOwnerClass());
                groupStorageName = Permission.getStorageName(Group.class, condition.getPropertyClass());
            } else {
                expandDevices = Device.class.isAssignableFrom(condition.getPropertyClass());
                groupStorageName = Permission.getStorageName(condition.getOwnerClass(), Group.class);
            }

            result.append(" UNION ");

            result.append("SELECT DISTINCT ");
            if (!expandDevices) {
                result.append(groupStorageName).append('.');
            }
            result.append(outputKey);
            result.append(" FROM ");
            result.append(groupStorageName);

            result.append(" INNER JOIN (");
            result.append("SELECT id as parentid, id as groupid FROM ");
            result.append(getStorageName(Group.class));
            result.append(" UNION ");
            result.append("SELECT groupid as parentid, id as groupid FROM ");
            result.append(getStorageName(Group.class));
            result.append(" WHERE groupid IS NOT NULL");
            result.append(" UNION ");
            result.append("SELECT g2.groupid as parentid, g1.id as groupid FROM ");
            result.append(getStorageName(Group.class));
            result.append(" AS g2");
            result.append(" INNER JOIN ");
            result.append(getStorageName(Group.class));
            result.append(" AS g1 ON g2.id = g1.groupid");
            result.append(" WHERE g2.groupid IS NOT NULL");
            result.append(") AS all_groups ON ");
            result.append(groupStorageName);
            result.append(".groupid = all_groups.parentid");

            if (expandDevices) {
                result.append(" INNER JOIN (");
                result.append("SELECT groupid as parentid, id as deviceid FROM ");
                result.append(getStorageName(Device.class));
                result.append(" WHERE groupid IS NOT NULL");
                result.append(") AS devices ON all_groups.groupid = devices.parentid");
            }

            result.append(" WHERE ");
            result.append(conditionKey); // TODO handle search for device / group
            result.append(" = :");
            result.append(conditionKey);

        }

        return result.toString();
    }

}
