import org.traccar.Server;

public class Main {

    public static void main(String[] args) throws Exception {

        final Server service = new Server();
        service.init(args);

        System.out.println("starting server...");
        service.start();

        // Shutdown server properly
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("shutting down server...");
                service.stop();
            }
        });

    }
}
