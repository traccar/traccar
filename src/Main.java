import org.traccar.Server;
import org.traccar.helper.Log;

public class Main {

    public static void main(String[] args) throws Exception {

        final Server service = new Server();
		String[] argss = new String[1];
		argss[0] = "setup\\windows\\windows.cfg";
        service.init(argss);

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
