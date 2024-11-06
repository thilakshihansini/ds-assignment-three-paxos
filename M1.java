import java.io.IOException;
import java.util.Map;

public class M1 extends CouncilMember {
    public M1(int id, int port, Map<Integer, Integer> memberPorts) throws IOException {
        super(id, port, memberPorts);
    }

    @Override
    protected void handleMessage(Message msg) {
        // Immediate response without delay
        processMessage(msg);
    }
}
