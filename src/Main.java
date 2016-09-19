import java.io.IOException;

public class Main {

    public static final String SERVER_VERSION = "0.7.1";
    public static Thread connectionHandlerThread;

    static Logger logger = new Logger(Logger.LOG_TYPE_NORMAL, "Main");
    static int port = 3000;
    static int locPollingRate = 30 * 60000;

    public static void main(String[] args) throws IOException {
        // check arguments
        if (args.length < 2 || args.length > 2) {
            System.out.println("Need port and location polling time in minutes");
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
        // parse polling time
        try {
            locPollingRate = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Need polling time in minutes");
            return;
        }
        // check polling time
        if (locPollingRate < 0 || locPollingRate > 65535) {
            System.out.println("Please enter a valid polling time");
            return;
        }
        if (locPollingRate == 0) {
            // everything is okay, start the server
            logger.log("\r\nServer started, listening on port " + port + ". Not polling location\r\n");
        } else {
            logger.log("\r\nServer started, listening on port " + port + ". Polling network location every " + locPollingRate + " minutes" + "\r\n");
        }
        connectionHandlerThread = new Thread(new ConnectionHandler(port, locPollingRate));
        connectionHandlerThread.start();
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
