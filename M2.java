import java.io.IOException;
import java.util.Map;

public class M2 extends CouncilMember {
    private boolean isOnline;

    public M2(int id, int port, Map<Integer, Integer> memberPorts, boolean isOnline) throws IOException {
        super(id, port, memberPorts);
        this.isOnline = isOnline;
    }

    @Override
    protected void handleMessage(Message msg) {
        if (!isOnline) {
            // Randomly drop message
            if (Math.random() < 0.5) {
                return;
            } else {
                // Simulate large delay
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // Immediate response without delay
        processMessage(msg);
    }
}
