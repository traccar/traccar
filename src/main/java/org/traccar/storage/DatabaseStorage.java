package org.traccar.storage;

import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DatabaseStorage extends Storage {

    private final DataSource dataSource;

    public DatabaseStorage(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> List<T> getObjects(Class<T> clazz, Request request) throws StorageException {
        StringBuilder query = new StringBuilder("SELECT ");
        query.append(formatColumns(request.getColumns(), clazz, c -> c));
        query.append(" FROM ").append(getTableName(clazz));
        query.append(formatCondition(request.getCondition()));
        query.append(formatOrder(request.getOrder()));
        try {
            QueryBuilder builder = QueryBuilder.create(dataSource, query.toString());
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
        query.append(getTableName(entity.getClass()));
        query.append("(");
        query.append(formatColumns(request.getColumns(), entity.getClass(), c -> c));
        query.append(") VALUES (");
        query.append(formatColumns(request.getColumns(), entity.getClass(), c -> ':' + c));
        query.append(")");
        try {
            QueryBuilder builder = QueryBuilder.create(dataSource, query.toString(), true);
            builder.setObject(entity);
            return builder.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public <T> void updateObject(T entity, Request request) throws StorageException {
        StringBuilder query = new StringBuilder("UPDATE ");
        query.append(getTableName(entity.getClass()));
        query.append(" SET ");
        query.append(formatColumns(request.getColumns(), entity.getClass(), c -> c + " = :" + c));
        query.append(formatCondition(request.getCondition()));
        try {
            QueryBuilder builder = QueryBuilder.create(dataSource, query.toString());
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
        query.append(getTableName(clazz));
        query.append(formatCondition(request.getCondition()));
        try {
            QueryBuilder builder = QueryBuilder.create(dataSource, query.toString());
            for (Map.Entry<String, Object> variable : getConditionVariables(request.getCondition()).entrySet()) {
                builder.setValue(variable.getKey(), variable.getValue());
            }
            builder.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    private String getTableName(Class<?> clazz) throws StorageException {
        StorageName storageName = clazz.getAnnotation(StorageName.class);
        if (storageName == null) {
            throw new StorageException("StorageName annotation is missing");
        }
        return storageName.value();
    }

    private Map<String, Object> getConditionVariables(Condition genericCondition) {
        Map<String, Object> result = new HashMap<>();
        if (genericCondition instanceof Condition.Equals) {
            Condition.Equals condition = (Condition.Equals) genericCondition;
            result.put(condition.getVariable(), condition.getValue());
        } else if (genericCondition instanceof Condition.Between) {
            Condition.Between condition = (Condition.Between) genericCondition;
            result.put(condition.getFromVariable(), condition.getFromValue());
            result.put(condition.getToVariable(), condition.getToValue());
        }
        return result;
    }

    private String formatColumns(Columns columns, Class<?> clazz, Function<String, String> mapper) {
        return columns.getColumns(clazz).stream().map(mapper).collect(Collectors.joining(", "));
    }

    private String formatCondition(Condition genericCondition) {
        StringBuilder result = new StringBuilder();
        if (genericCondition != null) {
            result.append(" WHERE ");
            if (genericCondition instanceof Condition.Equals) {

                Condition.Equals condition = (Condition.Equals) genericCondition;
                result.append(condition.getColumn());
                result.append(" == :");
                result.append(condition.getVariable());

            } else if (genericCondition instanceof Condition.Between) {

                Condition.Between condition = (Condition.Between) genericCondition;
                result.append(condition.getColumn());
                result.append(" BETWEEN :");
                result.append(condition.getFromVariable());
                result.append(" AND :");
                result.append(condition.getToVariable());

            } else if (genericCondition instanceof Condition.And) {

                Condition.And condition = (Condition.And) genericCondition;
                result.append(formatCondition(condition.getFirst()));
                result.append(" AND ");
                result.append(formatCondition(condition.getSecond()));

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

}
