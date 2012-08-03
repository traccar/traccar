import org.traccar.Server;
import org.traccar.helper.Log;

public class Main {

    public static void main(String[] args) throws Exception {
    	
    	final Server service = new Server();
    	
    	if(args.length > 0){
    		Log.info("Using parameterized config: " + args[0]);
    		service.init(args);
    	}
    	else{
    		String[] argsDefault = new String[]{"setup\\windows\\windows.cfg"}; 
    		Log.info("Using default config: " + argsDefault[0]);
    		service.init(argsDefault);	
    	}
        
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
