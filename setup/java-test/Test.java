public class Test {
    public static void main(String[] a) {
    	System.exit(Integer.parseInt(System.getProperty("java.version").split("\\.")[1]) >= 7 ? 0 : 1);
    }
}
