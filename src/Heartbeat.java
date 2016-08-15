import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;

public class Heartbeat extends Thread {

    private static final int HEART_RATE = 10000;
    private DataOutputStream out;
    private boolean running;
    private Logger logger;

    public void setRunning(boolean running) {
        this.running = running;
    }

    public Heartbeat(DataOutputStream out) {
        this.out = out;
        setRunning(true);
    }

    @Override
    public synchronized void run() {
        try {
            while (running) {
                if (out != null) {
                    out.writeInt(Protocol.HEARTBEAT);
                    out.writeInt(8); // client expects size, so send some int
                    out.flush();
                }
                Thread.sleep(HEART_RATE);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            running = false;
        }
    }
}
