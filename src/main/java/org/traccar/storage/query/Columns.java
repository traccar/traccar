package org.traccar.storage.query;

import org.traccar.storage.QueryExtended;
import org.traccar.storage.QueryIgnore;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Columns {

    public abstract List<String> getColumns(Class<?> clazz, String type);

    protected List<String> getAllColumns(Class<?> clazz, String type) {
        List<String> columns = new LinkedList<>();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            int parameterCount = type.equals("set") ? 1 : 0;
            if (method.getName().startsWith(type) && method.getParameterTypes().length == parameterCount
                    && !method.isAnnotationPresent(QueryIgnore.class)
                    && !method.isAnnotationPresent(QueryExtended.class)
                    && !method.getName().equals("getClass")) {
                columns.add(method.getName().substring(3).toLowerCase());
            }
        }
        return columns;
    }

    public static class All extends Columns {
        @Override
        public List<String> getColumns(Class<?> clazz, String type) {
            return getAllColumns(clazz, type);
        }
    }

    public static class Include extends Columns {
        private final List<String> columns;

        public Include(String... columns) {
            this.columns = Arrays.stream(columns).collect(Collectors.toList());
        }

        @Override
        public List<String> getColumns(Class<?> clazz, String type) {
            return columns;
        }
    }

    public static class Exclude extends Columns {
        private final Set<String> columns;

        public Exclude(String... columns) {
            this.columns = Arrays.stream(columns).collect(Collectors.toSet());
        }

        @Override
        public List<String> getColumns(Class<?> clazz, String type) {
            return getAllColumns(clazz, type).stream()
                    .filter(column -> !columns.contains(column))
                    .collect(Collectors.toList());
        }
    }

}
