package org.traccar.web;

import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.traccar.helper.Log;
import org.traccar.model.MiscFormatter;

public class CsvBuilder {

    private static final String LINE_ENDING = "\r\n";
    private static final String SEPARATOR = ";";
    private static final DateTimeFormatter DATE_FORMAT = ISODateTimeFormat.dateTime();

    private StringBuilder builder = new StringBuilder();

    private void addLineEnding() {
        builder.append(LINE_ENDING);
    }
    private void addSeparator() {
        builder.append(SEPARATOR);
    }

    private SortedSet<Method> getSortedMethods(Object object) {
        Method[] methodArray = object.getClass().getMethods();
        SortedSet<Method> methods = new TreeSet<Method>(new Comparator<Method>() {
            @Override
            public int compare(Method m1, Method m2) {
                return m1.getName().compareTo(m2.getName());
            }
        });
        methods.addAll(Arrays.asList(methodArray));
        return methods;
    }

    public void addLine(Object object) {

        SortedSet<Method> methods = getSortedMethods(object);

        for (Method method : methods) {
            if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
                try {
                    if (method.getReturnType().equals(boolean.class)) {
                        builder.append((Boolean) method.invoke(object));
                        addSeparator();
                    } else if (method.getReturnType().equals(int.class)) {
                        builder.append((Integer) method.invoke(object));
                        addSeparator();
                    } else if (method.getReturnType().equals(long.class)) {
                        builder.append((Long) method.invoke(object));
                        addSeparator();
                    } else if (method.getReturnType().equals(double.class)) {
                        builder.append((Double) method.invoke(object));
                        addSeparator();
                    } else if (method.getReturnType().equals(String.class)) {
                        builder.append((String) method.invoke(object));
                        addSeparator();
                    } else if (method.getReturnType().equals(Date.class)) {
                        Date value = (Date) method.invoke(object);
                        builder.append(DATE_FORMAT.print(new DateTime(value)));
                        addSeparator();
                    } else if (method.getReturnType().equals(Map.class)) {
                        Map value = (Map) method.invoke(object);
                        if (value != null) {
                            builder.append(MiscFormatter.toJson(value).toString());
                            addSeparator();
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException error) {
                    Log.warning(error);
                }
            }
        }
        addLineEnding();
    }

    public void addHeaderLine(Object object) {

        SortedSet<Method> methods = getSortedMethods(object);

        for (Method method : methods) {
            if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
                String name = Introspector.decapitalize(method.getName().substring(3));
                if (!name.equals("class")) {
                    builder.append(name);
                    addSeparator();
                }
            }
        }
        addLineEnding();
    }

    public void addArray(Collection<?> array) {
        for (Object object : array) {
            switch (object.getClass().getSimpleName().toLowerCase()) {
                case "string":
                    builder.append(object.toString());
                    addLineEnding();
                    break;
                case "long":
                    builder.append((long) object);
                    addLineEnding();
                    break;
                case "double":
                    builder.append((double) object);
                    addLineEnding();
                    break;
                case "boolean":
                    builder.append((boolean) object);
                    addLineEnding();
                    break;
                default:
                    addLine(object);
                    break;
            }
        }
    }

    public String build() {
        return builder.toString();
    }
}
