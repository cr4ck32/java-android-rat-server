import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

public class ProgressHandler extends Thread {
    private int progress;
    private int size;
    private boolean showProgress = true;
    private InputStream in;
    private long startTime;
    private static final long TIME_OUT = 10000;
    private Logger logger;
    private String clientID;

    public ProgressHandler(String clientID, int size, InputStream in) {
        logger = new Logger(Main.LOG_TYPE_PROGRESS, "progress");
        this.clientID = clientID;
        this.size = size;
        this.in = in;
        startTime = Calendar.getInstance().getTimeInMillis();
        logger.log("start " + startTime);
    }

    public void setProgress(int progress) {
        this.progress = progress;
        startTime = Calendar.getInstance().getTimeInMillis();
    }

    public void run() {
        int percent = size / 100;
        int percProg;
        while (showProgress) {
            percProg = progress / percent;
            logger.log("\r" + clientID + " " + progress + " " + size + " " + percProg + "%");
            try {
                if ((startTime + TIME_OUT) < Calendar.getInstance().getTimeInMillis()) {
                    if (in != null) {
                        in.close();
                    }
                    showProgress = false;
                }
                Thread.sleep(100);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        percProg = 100;
        logger.log("\r\n" + clientID + " " + progress + " " + size + " " + percProg + "%");
        logger.log("\r\n");
    }

    public void setShowProgress(boolean showProgress) {
        this.showProgress = showProgress;
    }
}
