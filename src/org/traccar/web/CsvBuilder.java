package org.traccar.web;

import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

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
    public void addLine(String value) {
        if (value != null) {
            builder.append(value);
        }
        addLineEnding();
    }

    public void addLine(long value) {
        builder.append(value);
        addLineEnding();
    }

    public void addLine(double value) {
        builder.append(value);
        addLineEnding();
    }

    public void addLine(boolean value) {
        builder.append(value);
        addLineEnding();
    }

    public void addField(String value) {
        builder.append(value);
        addSeparator();
    }

    public void addField(long value) {
        builder.append(value);
        addSeparator();
    }

    public void addField(int value) {
        builder.append(value);
        addSeparator();
    }

    public void addField(double value) {
        builder.append(value);
        addSeparator();
    }

    public void addField(boolean value) {
        builder.append(value);
        addSeparator();
    }

    public void addLine(Object object) {

        Method[] methods = object.getClass().getMethods();

        for (Method method : methods) {
            if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
                try {
                    if (method.getReturnType().equals(boolean.class)) {
                        addField((Boolean) method.invoke(object));
                    } else if (method.getReturnType().equals(int.class)) {
                        addField((Integer) method.invoke(object));
                    } else if (method.getReturnType().equals(long.class)) {
                        addField((Long) method.invoke(object));
                    } else if (method.getReturnType().equals(double.class)) {
                        addField((Double) method.invoke(object));
                    } else if (method.getReturnType().equals(String.class)) {
                        addField((String) method.invoke(object));
                    } else if (method.getReturnType().equals(Date.class)) {
                        Date value = (Date) method.invoke(object);
                        addField(DATE_FORMAT.print(new DateTime(value)));
                    } else if (method.getReturnType().equals(Map.class)) {
                        Map value = (Map) method.invoke(object);
                        if (value != null) {
                            addField(MiscFormatter.toJson(value).toString());
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
        Method[] methods = object.getClass().getMethods();

        for (Method method : methods) {
            if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
                String name = Introspector.decapitalize(method.getName().substring(3));
                if (!name.equals("class")) {
                    addField(name);
                }
            }
        }
        addLineEnding();
    }

    public void addArray(Collection<?> array) {
        for (Object object : array) {
            switch (object.getClass().getSimpleName().toLowerCase()) {
                case "string":
                    addLine(object.toString());
                    break;
                case "long":
                    addLine((long) object);
                    break;
                case "double":
                    addLine((double) object);
                    break;
                case "boolean":
                    addLine((boolean) object);
                    break;
                default:
                    addLine(object);
                    break;
            }
        }
    }

    public byte[] get() {
        return String.valueOf(builder).getBytes(StandardCharsets.UTF_8);
    }
}
