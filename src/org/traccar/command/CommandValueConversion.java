package org.traccar.command;

public interface CommandValueConversion<T> {
    public String convert(T value);
}
