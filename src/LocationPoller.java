import java.io.DataOutputStream;

public class LocationPoller extends Thread {

    private static final int POLLING_RATE = 15 * 60000;
    private final DataOutputStream out;
    private boolean running;

    public void setRunning(boolean running) {
        this.running = running;
    }

    public LocationPoller(DataOutputStream out) {
        this.out = out;
        setRunning(true);
    }

    @Override
    public synchronized void run() {
        try {
            while (running) {
                say("location single");
                Thread.sleep(POLLING_RATE);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
                    e.printStackTrace();
                }
            }
        }
    }
}


