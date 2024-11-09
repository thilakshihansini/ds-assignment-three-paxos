import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;


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

    @Test
    public void testSingleProposalM2Offline() throws Exception {
        // set member 2 response proposal to simulate poor internet at home
        members[1].responseProfile = CouncilMember.ResponseProfile.SLOW;

        startNodes();

        // Initiate proposal
        members[7].propose("Candidate_A");

        endNodes();

        // Assertion: The stream contains records of each member successfully learning the candidate
        // `Member i learned value: Candidate_A`
        // Further, the learnedValue attribute of each member should be "Candidate_A"
        for (int i = 0; i < 9; i++) {
            if (i == 1) { continue; }
            assertTrue(out.toString().contains("Member " + (i + 1) + " learned value: Candidate_A"));
            assertEquals("Candidate_A", members[i].learnedValue);
        }
    }

    @Test
    public void testSingleProposalM3Offline() throws Exception {
        // set member 3 response proposal to simulate disconnection while camping
        members[2].responseProfile = CouncilMember.ResponseProfile.OFFLINE;

        startNodes();

        // Initiate proposal
        members[7].propose("Candidate_A");

        endNodes();

        // Assertion: The stream contains records of each member successfully learning the candidate
        // `Member i learned value: Candidate_A`
        // Further, the learnedValue attribute of each member should be "Candidate_A"
        for (int i = 0; i < 9; i++) {
            if (i == 2) { continue; }
            assertTrue(out.toString().contains("Member " + (i + 1) + " learned value: Candidate_A"));
            assertEquals("Candidate_A", members[i].learnedValue);
        }
    }

    @Test
    public void testMultipleProposals() throws Exception {
        startNodes();

        // Initiate proposals
        members[7].propose("Candidate_A");
        members[8].propose("Candidate_B");

        endNodes();

        // Assertion: The stream contains records of each member successfully learning the candidate
        // `Member i learned value: Candidate_A` and `Member i learned value: Candidate_B`
        // Further, the learnedValue attribute of each member should be "Candidate_A" or "Candidate_B"
        // and this should be consistent across all members
        String learnedCandidate = members[0].learnedValue;
        assertTrue(learnedCandidate.equals("Candidate_A") || learnedCandidate.equals("Candidate_B"));
        for (int i = 0; i < 9; i++) {
            assertTrue(out.toString().contains("Member " + (i + 1) + " learned value: " + learnedCandidate));
            assertEquals(learnedCandidate, members[i].learnedValue);
        }
    }

    @Test
    public void testMultipleProposalsOverHalf() throws Exception {
        startNodes();

        // Initiate proposals
        members[0].propose("Candidate_A");
        members[1].propose("Candidate_B");
        members[2].propose("Candidate_C");
        members[3].propose("Candidate_D");
        members[4].propose("Candidate_E");
        members[5].propose("Candidate_F");

        endNodes();

        // Assertion: The stream contains records of each member successfully learning the candidate
        // `Member i learned value: Candidate_X` where X is the candidate with the most votes
        // Further, the learnedValue attribute of each member should be the candidate with the most votes
        // and this should be consistent across all members
        String learnedCandidate = members[0].learnedValue;
        assertTrue(learnedCandidate.equals("Candidate_A") || learnedCandidate.equals("Candidate_B") ||
                learnedCandidate.equals("Candidate_C") || learnedCandidate.equals("Candidate_D") ||
                learnedCandidate.equals("Candidate_E") || learnedCandidate.equals("Candidate_F"));
        for (int i = 0; i < 9; i++) {
            assertTrue(out.toString().contains("Member " + (i + 1) + " learned value: " + learnedCandidate));
            assertEquals(learnedCandidate, members[i].learnedValue);
        }
    }

    @Test
    public void testMultipleProposalsM2M3Offline() throws Exception {
        // when member 1 and member 2 are offline/poor connection
        members[1].responseProfile = CouncilMember.ResponseProfile.SLOW;
        members[2].responseProfile = CouncilMember.ResponseProfile.OFFLINE;

        startNodes();

        // Initiate proposals
        members[7].propose("Candidate_A");
        members[8].propose("Candidate_B");

        endNodes();

        // Assertion: The stream contains records of each member successfully learning the candidate
        // `Member i learned value: Candidate_A` and `Member i learned value: Candidate_B`
        // Further, the learnedValue attribute of each member should be "Candidate_A" or "Candidate_B"
        // and this should be consistent across all members
        String learnedCandidate = members[0].learnedValue;
        assertTrue(learnedCandidate.equals("Candidate_A") || learnedCandidate.equals("Candidate_B"));
        for (int i = 0; i < 9; i++) {
            if (i == 2) { continue; }
            else if (i == 1) {
                // member 2 should either have learned the value or not, but it cannot have learned a different value
                if (members[i].learnedValue != null) {
                    assertEquals(learnedCandidate, members[i].learnedValue);
                }
                continue;
            }
            assertTrue(out.toString().contains("Member " + (i + 1) + " learned value: " + learnedCandidate));
            assertEquals(learnedCandidate, members[i].learnedValue);
        }
    }

    @Test
    public void testProposeAndGoOfflineM2() throws Exception {
        startNodes();

        // Initiate proposal by member 2
        members[1].propose("Candidate_A");

        // Change member 2 profile
        members[1].responseProfile = CouncilMember.ResponseProfile.SLOW;

        endNodes();

        // Assertion: The voting process could go any way here,  but the learned value for each
        // member should be consistent
        String learnedCandidate = members[0].learnedValue;
        for (int i = 0; i < 9; i++) {
            assertEquals(learnedCandidate, members[i].learnedValue);
        }
    }

    @Test
    public void testProposeAndGoOfflineWithDelayM2() throws Exception {
        startNodes();

        // Initiate proposal by member 2
        members[1].propose("Candidate_A");

        // sleep for a while before member 2 goes offline
        Thread.sleep(2000);

        // Change member 2 profile
        members[1].responseProfile = CouncilMember.ResponseProfile.SLOW;

        endNodes();

        // Assertion: The voting process could go any way here,  but the learned value for each
        // member should be consistent
        String learnedCandidate = members[0].learnedValue;
        for (int i = 0; i < 9; i++) {
            assertEquals(learnedCandidate, members[i].learnedValue);
        }
    }

    @Test
    public void testProposeAndGoOfflineM2Multiple() throws Exception {
        startNodes();

        // Initiate proposal by member 2
        members[1].propose("Candidate_A");

        // Change member 2 profile
        members[1].responseProfile = CouncilMember.ResponseProfile.SLOW;

        // Initiate another proposal
        members[8].propose("Candidate_B");

        endNodes();

        // Assertion: The voting process could go any way here,  but the learned value for each
        // member should be consistent
        String learnedCandidate = members[0].learnedValue;
        for (int i = 0; i < 9; i++) {
            if (i == 1) { continue; }
            assertEquals(learnedCandidate, members[i].learnedValue);
        }
    }

    @Test
    public void testProposeAndGoOfflineWithDelayM2Multiple() throws Exception {
        startNodes();

        // Initiate proposal by member 2
        members[1].propose("Candidate_A");

        // Initiate another proposal
        members[8].propose("Candidate_B");

        // sleep for a while before member 2 goes offline
        Thread.sleep(2000);

        // Change member 2 profile
        members[1].responseProfile = CouncilMember.ResponseProfile.SLOW;

        endNodes();

        // Assertion: The voting process could go any way here,  but the learned value for each
        // member should be consistent
        String learnedCandidate = members[0].learnedValue;
        for (int i = 0; i < 9; i++) {
            if (i == 1) { continue; }
            assertEquals(learnedCandidate, members[i].learnedValue);
        }
    }

    @Test
    public void testProposeAndGoOfflineM3() throws Exception {
        startNodes();

        // Initiate proposal by member 3
        members[2].propose("Candidate_A");

        // Stop member 3
        members[2].responseProfile = CouncilMember.ResponseProfile.OFFLINE;

        endNodes();

        // Assertion: The voting process should have stalled since member 3 went offline
        // and no other proposal was made.
        for (int i = 0; i < 9; i++) {
            assertNull(members[i].learnedValue);
        }
    }

    @Test
    public void testProposeAndGoOfflineM3Multiple() throws Exception {
        startNodes();

        // Initiate proposal by member 3
        members[2].propose("Candidate_A");

        // Stop member 3
        members[2].responseProfile = CouncilMember.ResponseProfile.OFFLINE;

        // Initiate another proposal
        members[8].propose("Candidate_B");

        endNodes();

        // Assertion: The proposal for Candidate_B should have been accepted by all members
        // since member 3 went offline
        for (int i = 0; i < 9; i++) {
            if (i == 2) {continue;}
            assertEquals("Candidate_B", members[i].learnedValue);
        }
    }

    @Test
    public void testImmediateResponseAll()  throws Exception {
        // set all members to respond immediately
        for (int i = 0; i < 9; i++) {
            members[i].responseProfile = CouncilMember.ResponseProfile.IMMEDIATE;
        }

        startNodes();

        // Initiate proposal
        members[7].propose("Candidate_A");
        members[8].propose("Candidate_B");

        endNodes();

        // Assertion: The stream contains records of each member successfully learning the candidate
        // `Member i learned value: Candidate_A` and `Member i learned value: Candidate_B`
        // Further, the learnedValue attribute of each member should be "Candidate_A" or "Candidate_B"
        // and this should be consistent across all members
        String learnedCandidate = members[0].learnedValue;
        assertTrue(learnedCandidate.equals("Candidate_A") || learnedCandidate.equals("Candidate_B"));
        for (int i = 0; i < 9; i++) {
            assertTrue(out.toString().contains("Member " + (i + 1) + " learned value: " + learnedCandidate));
            assertEquals(learnedCandidate, members[i].learnedValue);
        }
    }

    @Test
    public void testImmediateResponseAllProposeAndGoOfflineM2() throws Exception {
        // set all members to respond immediately
        for (int i = 0; i < 9; i++) {
            members[i].responseProfile = CouncilMember.ResponseProfile.IMMEDIATE;
        }

        startNodes();

        // Initiate proposal by member 2
        members[1].propose("Candidate_A");

        // Change member 2 profile
        members[1].responseProfile = CouncilMember.ResponseProfile.SLOW;

        endNodes();

        // Assertion: The voting process could go any way here,  but the learned value for each
        // member should be consistent
        String learnedCandidate = members[0].learnedValue;
        for (int i = 0; i < 9; i++) {
            assertEquals(learnedCandidate, members[i].learnedValue);
        }
    }

    @Test
    public void testImmediateResponseAllProposeAndGoOfflineWithDelayM2Multiple() throws Exception {
        // set all members to respond immediately
        for (int i = 0; i < 9; i++) {
            members[i].responseProfile = CouncilMember.ResponseProfile.IMMEDIATE;
        }

        startNodes();

        // Initiate proposal by member 2
        members[1].propose("Candidate_A");

        // Initiate another proposal
        members[8].propose("Candidate_B");

        // sleep for a while before member 2 goes offline
        Thread.sleep(2000);

        // Change member 2 profile
        members[1].responseProfile = CouncilMember.ResponseProfile.SLOW;

        endNodes();

        // Assertion: The voting process could go any way here,  but the learned value for each
        // member should be consistent
        String learnedCandidate = members[0].learnedValue;
        for (int i = 0; i < 9; i++) {
            if (i == 1) { continue; }
            assertEquals(learnedCandidate, members[i].learnedValue);
        }
    }

    @Test
    public void testImmediateResponseAllProposeAndGoOfflineM3() throws Exception {
        // set all members to respond immediately
        for (int i = 0; i < 9; i++) {
            members[i].responseProfile = CouncilMember.ResponseProfile.IMMEDIATE;
        }

        startNodes();

        // Initiate proposal by member 3
        members[2].propose("Candidate_A");

        // Stop member 3
        members[2].responseProfile = CouncilMember.ResponseProfile.OFFLINE;

        endNodes();

        // Assertion: The voting process should have stalled since member 3 went offline
        // and no other proposal was made.
        for (int i = 0; i < 9; i++) {
            assertNull(members[i].learnedValue);
        }
    }

    @Test
    public void testImmediateResponseAllProposeAndGoOfflineM3Multiple() throws Exception {
        // set all members to respond immediately
        for (int i = 0; i < 9; i++) {
            members[i].responseProfile = CouncilMember.ResponseProfile.IMMEDIATE;
        }

        startNodes();

        // Initiate proposal by member 3
        members[2].propose("Candidate_A");

        // Stop member 3
        members[2].responseProfile = CouncilMember.ResponseProfile.OFFLINE;

        // Initiate another proposal
        members[8].propose("Candidate_B");

        endNodes();

        // Assertion: The proposal for Candidate_B should have been accepted by all members
        // since member 3 went offline
        for (int i = 0; i < 9; i++) {
            if (i == 2) {continue;}
            assertEquals("Candidate_B", members[i].learnedValue);
        }
    }

    @Test
    public void testSmallDelayAll()  throws Exception {
        // set all members to respond with a small delay
        for (int i = 0; i < 9; i++) {
            members[i].responseProfile = CouncilMember.ResponseProfile.DELAY_SMALL;
        }

        startNodes();

        // Initiate proposal
        members[7].propose("Candidate_A");
        members[8].propose("Candidate_B");

        endNodes();

        // Assertion: The stream contains records of each member successfully learning the candidate
        // `Member i learned value: Candidate_A` and `Member i learned value: Candidate_B`
        // Further, the learnedValue attribute of each member should be "Candidate_A" or "Candidate_B"
        // and this should be consistent across all members
        String learnedCandidate = members[0].learnedValue;
        assertTrue(learnedCandidate.equals("Candidate_A") || learnedCandidate.equals("Candidate_B"));
        for (int i = 0; i < 9; i++) {
            assertTrue(out.toString().contains("Member " + (i + 1) + " learned value: " + learnedCandidate));
            assertEquals(learnedCandidate, members[i].learnedValue);
        }
    }

    @Test
    public void testLargeDelayAll()  throws Exception {
        // set all members to respond with a large delay
        for (int i = 0; i < 9; i++) {
            members[i].responseProfile = CouncilMember.ResponseProfile.DELAY_LARGE;
        }

        startNodes();

        // Initiate proposal
        members[7].propose("Candidate_A");
        members[8].propose("Candidate_B");

        endNodes();

        // Assertion: The stream contains records of each member successfully learning the candidate
        // `Member i learned value: Candidate_A` and `Member i learned value: Candidate_B`
        // Further, the learnedValue attribute of each member should be "Candidate_A" or "Candidate_B"
        // and this should be consistent across all members
        String learnedCandidate = members[0].learnedValue;
        assertTrue(learnedCandidate.equals("Candidate_A") || learnedCandidate.equals("Candidate_B"));
        for (int i = 0; i < 9; i++) {
            assertTrue(out.toString().contains("Member " + (i + 1) + " learned value: " + learnedCandidate));
            assertEquals(learnedCandidate, members[i].learnedValue);
        }
    }
}
