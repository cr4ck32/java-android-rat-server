import java.io.IOException;

public class Main {

    public static final String SERVER_VERSION = "0.6";
    public static final int LOG_TYPE_PROGRESS = 0;
    public static final int LOG_TYPE_NORMAL = 1;
    public static final int LOG_TYPE_LOCATIONS = 2;
    public static final int LOG_TYPE_USER_OVERVIEW = 3;

    static Logger logger = new Logger(LOG_TYPE_NORMAL, "Main");
    static int port = 3000;

    public static void main(String[] args) throws IOException {
        // check arguments
        if (args.length == 0 || args.length > 1) {
            System.out.println("Need port number to listen on");
            return;
        }
        // parse port
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Need port number to listen on");
            return;
        }
        // check valid port
        if (port <= 0 || port > 65535) {
            System.out.println("Please enter a valid port number");
            return;
        }
        // everything is okay, start the server
        logger.log("\r\nServer started, listening on port " + port + "\r\n");
        new Thread(new ConnectionHandler(port)).start();
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutDown()));
    }

    static class ShutDown extends Thread {
        @Override
        public void run() {
            System.out.println("\r\nServer stopped\r\n");
            logger.log("\r\nServer stopped\r\n");
        }
    }


}
