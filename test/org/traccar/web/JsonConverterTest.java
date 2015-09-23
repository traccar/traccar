package org.traccar.web;

import java.io.StringReader;
import java.text.ParseException;
import java.util.Date;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.traccar.model.Factory;

public class JsonConverterTest {

    @Test
    public void primitiveConversion() throws ParseException {

        Primitives o = JsonConverter.objectFromJson(new StringReader(
                "{" +
                "\"boolean\": true, " +
                "\"int\": 42, " +
                "\"double\": 41.99, " +
                "\"string\": \"discworld\", " +
                "\"date\":\"2015-07-09T19:02:17.000Z\"" +
                "}"),
                new Primitives());

        assertEquals(true, o.getBoolean());
        assertEquals(42, o.getInt());
        assertEquals(41.99, o.getDouble(), 0.001);
        assertEquals("discworld", o.getString());
        assertEquals(1436468537000L, o.getDate().getTime());

    }

    public static class Primitives implements Factory {
        
        @Override
        public Primitives create() {
            return new Primitives();
        }

        private boolean b;
        public boolean getBoolean() { return b; }
        public void setBoolean(boolean b) { this.b = b; }
        
        private int i;
        public int getInt() { return i; }
        public void setInt(int i) { this.i = i; }
        
        private long l;
        public long getLong() { return l; }
        public void setLong(long l) { this.l = l; }
        
        private double d;
        public double getDouble() { return d; }
        public void setDouble(double d) { this.d = d; }
        
        private String s;
        public String getString() { return s; }
        public void setString(String s) { this.s = s; }
        
        private Date t;
        public Date getDate() { return t; }
        public void setDate(Date t) { this.t = t; }

    }

}
