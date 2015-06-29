package org.traccar.command;

public class Duration {

    public enum TimeUnit {
        SECOND("s"), MINUTE("m"), HOUR("h");

        private final String commandFormat;

        TimeUnit(String commandFormat) {
            this.commandFormat = commandFormat;
        }

        public String getCommandFormat() {
            return commandFormat;
        }
    }


    private TimeUnit unit;
    private int value;

    public TimeUnit getUnit() {
        return unit;
    }

    public void setUnit(TimeUnit unit) {
        this.unit = unit;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
