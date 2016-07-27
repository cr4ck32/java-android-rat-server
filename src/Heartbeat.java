import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by stofstik on 21-1-15.
 */
public class Heartbeat extends Thread {

    private static final int HEART_RATE = 10000;
    private DataOutputStream out;
    private boolean running = true;
    private int threadNum;
    private Logger logger;

    public void setRunning(boolean running) {
        this.running = running;
    }

    public Heartbeat(DataOutputStream out, String clientID, int threadNum) {
        this.out = out;
        this.threadNum = threadNum;
        // logger = new Logger(clientID + " Thread " + threadNum);
    }

    @Override
    public synchronized void run() {
        try {
            while (running) {
                if (out != null) {
                    out.writeInt(Protocol.HEARTBEAT);
                    out.writeInt(1); // client expects size, so send some int
                    out.flush();
                }
                Thread.sleep(HEART_RATE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
