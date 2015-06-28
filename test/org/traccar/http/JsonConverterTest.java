package org.traccar.http;

import org.junit.Test;
import org.traccar.http.JsonConverter;
import org.traccar.model.Factory;

import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JsonConverterTest {

    private <T extends Factory> T convert(String jsonString, T prototype) throws ParseException {
        Reader r = new StringReader(
                jsonString);

        return JsonConverter.objectFromJson(r, prototype);
    }

    @Test
    public void primitiveConversion() throws ParseException {

        AllPrimitives o = convert("{" +
                        "\"aBoolean\": true, " +
                        "\"anInt\": 42, " +
                        "\"aDouble\": 41.99, " +
                        "\"aString\": \"discworld\", " +
                        "\"aDate\":\"2015-07-09T19:02:17\"" +
                        "}",
                new AllPrimitives());

        assertEquals(true, o.getaBoolean());
        assertEquals(42, o.getAnInt());
        assertEquals(41.99, o.getaDouble(), 0.001);
        assertEquals("discworld", o.getaString());

        Calendar c = Calendar.getInstance();
        c.setTime(o.getaDate());
        assertEquals(2015, c.get(Calendar.YEAR));
        assertEquals(Calendar.JULY, c.get(Calendar.MONTH));
        assertEquals(9, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(19, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(2, c.get(Calendar.MINUTE));
        assertEquals(17, c.get(Calendar.SECOND));
    }

    public static class AllPrimitives implements Factory {

        private boolean aBoolean;
        private int anInt;
        private double aDouble;
        private String aString;
        private Date aDate;


        @Override
        public Object create() {
            return new AllPrimitives();
        }

        public boolean getaBoolean() {
            return aBoolean;
        }

        public void setaBoolean(boolean aBoolean) {
            this.aBoolean = aBoolean;
        }

        public int getAnInt() {
            return anInt;
        }

        public void setAnInt(int anInt) {
            this.anInt = anInt;
        }

        public double getaDouble() {
            return aDouble;
        }

        public void setaDouble(double aDouble) {
            this.aDouble = aDouble;
        }

        public String getaString() {
            return aString;
        }

        public void setaString(String aString) {
            this.aString = aString;
        }

        public Date getaDate() {
            return aDate;
        }

        public void setaDate(Date aDate) {
            this.aDate = aDate;
        }
    }


    @Test
    public void enumConversion() throws ParseException {
        ObjectWithEnum o = convert("{\"anEnum\": \"VALUE2\"}", new ObjectWithEnum());
        assertEquals(TestEnum.VALUE2, o.getAnEnum());
    }


    public enum TestEnum {
        VALUE1, VALUE2
    }

    public static class ObjectWithEnum implements Factory {
        private TestEnum anEnum;

        public TestEnum getAnEnum() {
            return anEnum;
        }

        public void setAnEnum(TestEnum anEnum) {
            this.anEnum = anEnum;
        }

        @Override
        public Object create() {
            return new ObjectWithEnum();
        }
    }


    @Test
    public void nestedObjectsConversion() throws ParseException {
        NestedObjects o = convert("{\"name\": \"Rincewind\", \"nestedObject\": {\"anEnum\":\"VALUE1\"}}", new NestedObjects());
        assertEquals("Rincewind", o.getName());
        assertNotNull("The nested object should be populated", o.getNestedObject());
        assertEquals(TestEnum.VALUE1, o.getNestedObject().getAnEnum());
    }

    public static class NestedObjects implements Factory {

        private String name;
        private ObjectWithEnum nestedObject;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ObjectWithEnum getNestedObject() {
            return nestedObject;
        }

        public void setNestedObject(ObjectWithEnum nestedObject) {
            this.nestedObject = nestedObject;
        }

        @Override
        public Object create() {
            return new NestedObjects();
        }
    }
}
