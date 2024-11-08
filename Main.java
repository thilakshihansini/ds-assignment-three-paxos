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
        
        for (int i = 0; i < 9; i++) {
            members[i] = new CouncilMember(i + 1, 5000 + i + 1, memberPorts);
        }
        members[0].responseProfile = CouncilMember.ResponseProfile.IMMEDIATE;
        members[1].responseProfile = CouncilMember.ResponseProfile.SLOW;
        members[2].responseProfile = CouncilMember.ResponseProfile.OFFLINE;

        // Start all member threads
        for (CouncilMember member : members) {
            new Thread(member).start();
        }

        // Give the threads time to start up
        Thread.sleep(1000);

        // Initiate proposals
        members[7].propose("Candidate_A");
        members[8].propose("Candidate_B");

        // Wait for consensus
        Thread.sleep(6000);

        // Stop all member threads
        for (CouncilMember member : members) {
            member.isRunning = false;
            member.serverSocket.close();
        }
    }
}
