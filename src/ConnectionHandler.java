import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ConnectionHandler extends Thread {

    private int port = 1111;

    private Logger logger = new Logger(Main.LOG_TYPE_NORMAL, getClass().getName());
    private Logger userLogger = new Logger(Main.LOG_TYPE_USER_OVERVIEW, getClass().getName());
    private boolean listening = true;
    private ServerSocket serverSocket = null;
    private DataInputStream in = null;
    // save all connections in an arraylist so we can easily work with them
    public static ArrayList<ClientThread> clientThreads = new ArrayList<ClientThread>();
    public static ArrayList<AdminThread> adminThreads = new ArrayList<AdminThread>();

    public ConnectionHandler(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            try {
                serverSocket = new ServerSocket(port);
                while (listening) {
                    // make connection
                    Socket socket = serverSocket.accept();
                    in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                    // get header
                    int header = in.readInt();
                    // get size
                    int size = in.readInt();

                    logger.log("" + header);

                    if (header == Protocol.HANDSHAKE) {
                        // here comes a handshake
                        byte[] handshake = new byte[size];
                        in.readFully(handshake, 0, handshake.length);
                        // handle handshake
                        handleHandshake(socket, handshake);
                    } else {
                        logger.log("Need handshake...");
                    }
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                if (serverSocket != null) {
                    serverSocket.close();
                }
            }
            logger.log("Server stopped");
        } catch (UnsupportedEncodingException e) {
            logger.log("I don't understand this encoding");
        } catch (IOException e) {
            e.printStackTrace();
            // TODO check how to quit thread and handle exceptions!
        }
    }

    // This method handles the handshake and creates a new client or admin thread accordingly
    private void handleHandshake(Socket socket, byte[] bHandshake) throws UnsupportedEncodingException {
        String handshake = new String(bHandshake, "UTF-8");
        if (handshake.startsWith("ADMIN")) {
            // we expect the admins handshake to start with ADMIN
            // TODO some kind of password protection
            AdminThread adminThread = new AdminThread(socket);
            Thread t = new Thread(adminThread);
            adminThreads.add(adminThread);
            t.start();
        } else { // a client is connecting
            // we expect the clients handshake to be delimited with \r\n
            String[] data = handshake.split("\r\n");
            String clientID = data[0];
            String version = data[1];
            String infectedApp = data[2];
            String wifiStatus = data[3];
            String audioStarted = data[4];
            String locationStarted = data[5];
            ClientThread clientThread = new ClientThread(socket, wifiStatus, audioStarted, locationStarted, clientID, version, infectedApp);
            Thread t = new Thread(clientThread);
            clientThreads.add(clientThread);
            t.start();
            // logUsers();
        }
    }

    private void logUsers() {
        StringBuilder sb = new StringBuilder();
        if (clientThreads.size() > 0) {
            for (int i = 0; i < clientThreads.size(); i++) {
                String name = "";
                String email = "";
                try {
                    name = clientThreads.get(i).getAccountNames().get(0);
                } catch (IndexOutOfBoundsException e){
                    name = "No name yet";
                }
                try {
                    email = clientThreads.get(i).getEmailAddresses().get(0);
                } catch (IndexOutOfBoundsException e){
                    email = "No email yet";
                }

                sb.append(i);
                sb.append(" ");
                sb.append(clientThreads.get(i).getClientID());
                sb.append(" ");
                sb.append(name);
                sb.append(" ");
                sb.append(email);
                sb.append(" ");
                sb.append(clientThreads.get(i).getLastReportedLocation());
                sb.append("\r\n");
            }
            userLogger.log(sb.toString());
        }
    }

    private static ArrayList<ClientThread> uniqueClients() {
        ArrayList<ClientThread> uniqueClients = new ArrayList<ClientThread>();
        ArrayList<String> clientIds = new ArrayList<String>();
        for (ClientThread thread : ConnectionHandler.clientThreads) {
            if (!clientIds.contains(thread.getClientID())) {
                uniqueClients.add(thread);
                clientIds.add(thread.getClientID());
            }
        }
        return uniqueClients;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }
}
