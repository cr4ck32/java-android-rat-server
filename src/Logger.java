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
    public static final int LOG_TYPE_INSTALLED_APPS= 6;
    public static final int LOG_TYPE_SD_CARD = 7;
    public static final int LOG_TYPE_STATUS = 8;

    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss", Locale.getDefault());
    private ArrayList<String> accountNames = new ArrayList<String>();

    File threadsLogFile, globalLogFile, clientThreadLogFile, locationsFile, accountsFile, wifiAPFile, installedAppsFile, sdCardFile, statusFile;
    int logType;
    String clientId;

    public Logger(int logType, String clientId) {
        this.logType = logType;
        this.clientId = clientId;
        threadsLogFile = new File("threads.log");
        globalLogFile = new File("logfile.log");
        clientThreadLogFile = new File(clientId, "client-logfile.log");
        locationsFile = new File(clientId, "locations.log");
        accountsFile = new File(clientId, "accounts.log");
        wifiAPFile = new File(clientId, "wifi-aps.log");
        installedAppsFile = new File(clientId, "installed-apps.log");
        sdCardFile = new File(clientId, "sd-card.log");
        statusFile = new File(clientId, "last-status.log");
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
                    StringBuilder sbAccounts = new StringBuilder();
                    sbAccounts.append(formatter.format(Calendar.getInstance().getTime()));
                    sbAccounts.append(" ");
                    sbAccounts.append(clientId);
                    sbAccounts.append("\r\n");
                    sbAccounts.append(str);
                    FileUtils.writeStringToFile(accountsFile, sbAccounts.toString(), "UTF-8", false);
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
                case LOG_TYPE_INSTALLED_APPS:
                    StringBuilder sbInstApps = new StringBuilder();
                    sbInstApps.append(formatter.format(Calendar.getInstance().getTime()));
                    sbInstApps.append(" ");
                    sbInstApps.append(clientId);
                    sbInstApps.append("\r\n");
                    sbInstApps.append(str);
                    FileUtils.writeStringToFile(installedAppsFile, sbInstApps.toString(), "UTF-8", false);
                    break;
                case LOG_TYPE_SD_CARD:
                    StringBuilder sbSdCard = new StringBuilder();
                    sbSdCard.append(formatter.format(Calendar.getInstance().getTime()));
                    sbSdCard.append(" ");
                    sbSdCard.append(clientId);
                    sbSdCard.append("\r\n");
                    sbSdCard.append(str);
                    FileUtils.writeStringToFile(sdCardFile, sbSdCard.toString(), "UTF-8", false);
                    break;
                case LOG_TYPE_STATUS:
                    StringBuilder sbState = new StringBuilder();
                    sbState.append(formatter.format(Calendar.getInstance().getTime()));
                    sbState.append(" ");
                    sbState.append(clientId);
                    sbState.append("\r\n");
                    sbState.append(str);
                    FileUtils.writeStringToFile(statusFile, sbState.toString(), "UTF-8", false);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}