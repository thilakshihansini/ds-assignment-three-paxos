import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        Map<Integer, Integer> memberPorts = new HashMap<>();
        for (int i = 1; i <= 9; i++) {
            memberPorts.put(i, 5000 + i);
        }

        CouncilMember[] members = new CouncilMember[9];
        members[0] = new M1(1, 5001, memberPorts);
        members[1] = new M2(2, 5002, memberPorts, true); // M2 is online
        members[2] = new M3(3, 5003, memberPorts, false);  // M3 is disconnected
        
        // Members 4-9 are regular council members
        for (int i = 3; i < 9; i++) {
            members[i] = new CouncilMember(i + 1, 5000 + i + 1, memberPorts);
        }

        // Start all member threads
        for (CouncilMember member : members) {
            new Thread(member).start();
        }

        // Give the threads time to start up
        Thread.sleep(1000);

        // Initiate proposals
        members[0].propose("Candidate_A");
        members[1].propose("Candidate_B");

        // Wait for consensus
        Thread.sleep(20000);

        // Stop all member threads
        for (CouncilMember member : members) {
            member.isRunning = false;
            member.serverSocket.close();
        }
    }
}
