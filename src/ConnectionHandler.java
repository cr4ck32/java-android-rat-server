import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ConnectionHandler extends Thread {

    private int port = 1111;
    private int pollingRate = 30 * 60000;

    private Logger logger = new Logger(Logger.LOG_TYPE_NORMAL, getClass().getName());
    private boolean listening = true;
    private ServerSocket serverSocket = null;
    private DataInputStream in = null;
    // Save all connections in an arraylist so we can easily work with them
    public static ArrayList<ClientThread> clientThreads = new ArrayList<ClientThread>();
    public static ArrayList<AdminThread> adminThreads = new ArrayList<AdminThread>();

    public ConnectionHandler(int port, int pollingRate) {
        this.port = port;
        this.pollingRate = pollingRate;
    }

    @Override
    public void run() {
        // Create listening socket
        try {
            serverSocket = new ServerSocket(port);
            while (listening) {
                try {
                    // Listen for incoming connections
                    Socket socket = serverSocket.accept();
                    DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                    // Get header
                    int header = in.readInt();
                    // Get size
                    int size = in.readInt();

                    if (header == Protocol.HANDSHAKE) {
                        // Here comes a handshake
                        byte[] handshake = new byte[size];
                        in.readFully(handshake, 0, handshake.length);
                        // Handle handshake
                        handleHandshake(socket, handshake);
                    } else {
                        logger.log("Need handshake...");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(in != null){
                        in.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*@Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (listening) {
                // Make connection
                Socket socket = serverSocket.accept();
                in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                // Get header
                int header = in.readInt();
                // Get size
                int size = in.readInt();

                if (header == Protocol.HANDSHAKE) {
                    // Here comes a handshake
                    byte[] handshake = new byte[size];
                    in.readFully(handshake, 0, handshake.length);
                    // Handle handshake
                    handleHandshake(socket, handshake);
                } else {
                    logger.log("Need handshake...");
                }
            }
        } catch (UnsupportedEncodingException e) {
            logger.log("I don't understand this encoding");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.log("ConnectionHandler stopped");
        }
    }*/

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
//            String wifiStatus = data[3];
//            String audioStarted = data[4];
//            String locationStarted = data[5];
            ClientThread clientThread = new ClientThread(socket, clientID, version, infectedApp, pollingRate);
            Thread t = new Thread(clientThread);
            clientThreads.add(clientThread);
            t.start();
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

}
