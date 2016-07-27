import java.util.HashMap;

/**
 * Created by stofstik on 3-2-15.
 */
public class Protocol extends HashMap<String, Integer> {
    public static final int HEARTBEAT = 1000;
    public static final int HANDSHAKE = 2000;
    public static final int ADMIN = 3000;
    public static final int COMMAND = 3100;
    public static final int MESSAGE = 4000;
    public static final int FILE = 5000;

}
