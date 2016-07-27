import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Logger {
    // TODO log to logFile, make sure double connection log to the same log logFile but verbosely
    // use infected app as prefix for example
    //

    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss", Locale.getDefault());

    OutputStreamWriter outLog, outLocations, outUsers;
    File logFile, locationsFile, usersFile;
    int logType;
    String threadName;

    public Logger(int logType, String threadName) {
        this.logType = logType;
        this.threadName = threadName;
        logFile = new File("logfile" + ".log");
        locationsFile = new File("locations.log");
        usersFile = new File("users.log");
    }

    public void log(String str) {
        try {
            try {
                outLog = new OutputStreamWriter(new FileOutputStream(logFile, true));
                switch (logType) {
                    case Main.LOG_TYPE_PROGRESS:
                        System.out.print(str);
                        outLog.write(str);
                        break;
                    case Main.LOG_TYPE_NORMAL:
                        StringBuilder sb = new StringBuilder();
                        sb.append(formatter.format(Calendar.getInstance().getTime()));
                        sb.append(" ");
                        sb.append(threadName);
                        sb.append(" ");
                        sb.append(str);
                        // TODO here we should log to local logFile e.g. the threads own verbose log
                        // TODO and we should log to the global verbose log
                        // TODO if we start a Progress thread we can construct a Logger and use this class to create a downloads progress log
                        System.out.println(sb.toString());
                        outLog.write(sb.toString() + "\r\n");
                        break;
                    case Main.LOG_TYPE_LOCATIONS:
                        StringBuilder sbLoc = new StringBuilder();
                        sbLoc.append(formatter.format(Calendar.getInstance().getTime()));
                        sbLoc.append(" ");
                        sbLoc.append(threadName);
                        sbLoc.append(" ");
                        sbLoc.append(str);
                        outLocations = new OutputStreamWriter(new FileOutputStream(locationsFile, true));
                        outLocations.write(sbLoc.toString() + "\r\n");
                        break;
                    case Main.LOG_TYPE_USER_OVERVIEW:
                        StringBuilder sbUsers = new StringBuilder();
                        sbUsers.append(formatter.format(Calendar.getInstance().getTime()));
                        sbUsers.append("\r\n");
                        sbUsers.append(str);
                        outUsers = new OutputStreamWriter(new FileOutputStream(usersFile, false));
                        outUsers.write(sbUsers.toString());
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