public class Test {
    public static void main(String[] a) {
        String[] versions = System.getProperty("java.version").split("\\.");
        int major = Integer.parseInt(versions[0]);
        if (major == 1) {
            major = Integer.parseInt(versions[1]);
        }
    	System.exit(major >= 7 ? 0 : 1);
    }
}
