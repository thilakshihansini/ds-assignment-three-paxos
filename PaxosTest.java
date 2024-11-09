import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;


@SuppressWarnings("WrongPackageStatement")
public class PaxosTest {
    // save out the console output to a stream, rather than printing to the actual console window
	private final ByteArrayOutputStream out = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;

    private static final int MAX_WAIT_TIME = 15;
    private static Map<Integer, Integer> memberPorts;
    private CouncilMember[] members;

    @BeforeClass
    public static void setUpClass() throws Exception {
        memberPorts = new HashMap<>();
        for (int i = 1; i <= 9; i++) {
            memberPorts.put(i, 5000 + i);
        }
    }

    @Before
    public void setUp() throws Exception {
        // Capture console output during the test
        System.setOut(new PrintStream(out));

        members = new CouncilMember[9];
        
        for (int i = 0; i < 9; i++) {
            members[i] = new CouncilMember(i + 1, 5000 + i + 1, memberPorts);
        }
        // The default profile is a small delay for all members. However M1 will always
        // respond immediately
        members[0].responseProfile = CouncilMember.ResponseProfile.IMMEDIATE;
    }

    @After
    public void cleanUp() {
        System.setOut(originalOut);
    }

    public void startNodes() throws Exception {
        // Start all member threads
        for (CouncilMember member : members) {
            new Thread(member).start();
        }

        // Give the threads time to start up
        Thread.sleep(1000);
    }

    public void endNodes() throws Exception {
        boolean allFinished = false;
        // Wait for consensus
        for (int i = 0; i < MAX_WAIT_TIME; i++) {
            allFinished = true;
            for (CouncilMember member : members) {
                if (member.isRunning) {
                    allFinished = false;
                    break;
                }
            }
            if (allFinished) {
                break;
            }
            Thread.sleep(1000);
        }
        
        // Stop all member threads
        for (CouncilMember member : members) {
            member.isRunning = false;
            member.serverSocket.close();
        }
    }

    @Test
    public void testSuccess() throws Exception {
        // This test should always pass
        assertTrue(true);
    }


    @Test
    public void testSingleProposal() throws Exception {
        startNodes();

        // Initiate proposal
        members[7].propose("Candidate_A");

        endNodes();

        // Assertion: The stream contains records of each member successfully learning the candidate
        // `Member i learned value: Candidate_A`
        // Further, the learnedValue attribute of each member should be "Candidate_A"
        for (int i = 0; i < 9; i++) {
            assertTrue(out.toString().contains("Member " + (i + 1) + " learned value: Candidate_A"));
            assertEquals("Candidate_A", members[i].learnedValue);
        }
    }

}
