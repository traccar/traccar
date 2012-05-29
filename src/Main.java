import org.traccar.Server;
import org.traccar.helper.Log;

public class Main {

    public static void main(String[] args) throws Exception {

        final Server service = new Server();
        service.init(args);

        Log.info("starting server...");
        service.start();

        // Shutdown server properly
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Log.info("shutting down server...");
                service.stop();
            }
        });

    }
}
