package org.traccar.protocol.commands;

public interface CommandValueConversion<T> {
    public String convert(T value);
}
