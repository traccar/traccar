import net.sourceforge.opentracking.Daemon;

public class Main {

    public static void main(String[] args) throws Exception {

        Daemon service = new Daemon();
        service.init(args);
        service.start();
    }
}
