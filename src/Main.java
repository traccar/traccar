import org.traccar.Server;

public class Main {

    public static void main(String[] args) throws Exception {

        Server service = new Server();
        service.init(args);
        service.start();
    }
}
