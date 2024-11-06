import java.io.IOException;
import java.util.Map;

public class M3 extends CouncilMember {
    private boolean isConnected;

    public M3(int id, int port, Map<Integer, Integer> memberPorts, boolean isConnected) throws IOException {
        super(id, port, memberPorts);
        this.isConnected = isConnected;
    }

    @Override
    protected void handleMessage(Message msg) {
        if (!isConnected) {
            // Message doesn't reach M3
            return;
        }

        // we will keep the default delay in the handleMessage method
        super.handleMessage(msg);
    }
}
