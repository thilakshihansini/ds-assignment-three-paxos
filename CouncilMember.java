import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

@SuppressWarnings("WrongPackageStatement")
public class CouncilMember implements Runnable {
    public enum ResponseProfile {
        IMMEDIATE,
        DELAY_SMALL,
        DELAY_LARGE,
        SLOW,
        OFFLINE
    }
    protected ResponseProfile responseProfile = ResponseProfile.DELAY_SMALL;

    protected int id;
    protected int port;
    protected ServerSocket serverSocket;
    protected volatile boolean isRunning = true;

    // Map of member IDs to their ports, we maintain this to send messages to other members
    protected Map<Integer, Integer> memberPorts;
    protected List<Integer> memberIds;

    protected int promisedProposalNumber = -1;
    protected int acceptedProposalNumber = -1;
    protected String acceptedValue = null;

    protected int proposalNumber = 0;
    protected String proposalValue = null;
    protected Set<Integer> promisesReceived = Collections.synchronizedSet(new HashSet<>());
    protected Map<Integer, Integer> highestAcceptedProposalNumbers = Collections.synchronizedMap(new HashMap<>());
    protected Map<Integer, String> previousAcceptedValues = Collections.synchronizedMap(new HashMap<>());
    protected boolean acceptRequestSent = false;

    protected Set<Integer> acceptsReceived = Collections.synchronizedSet(new HashSet<>());
    protected boolean learnedValueSent = false;
    protected String learnedValue = null;

    public CouncilMember(int id, int port, Map<Integer, Integer> memberPorts) throws IOException {
        this.id = id;
        this.port = port;
        this.memberPorts = memberPorts;
        this.memberIds = new ArrayList<>(memberPorts.keySet());
        serverSocket = new ServerSocket(port);
    }

    public void run() {
        try {
            while (isRunning) {
                Socket socket = serverSocket.accept();
                // Handle each connection in a separate thread, so that the server can continue to accept new connections
                new Thread(new ConnectionHandler(socket)).start();
            }
        } catch (IOException e) {
            if (!isRunning) {
            } else {
                e.printStackTrace();
            }
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Runnable class to handle incoming connections
    private class ConnectionHandler implements Runnable {
        private Socket socket;

        public ConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                Message msg = (Message) in.readObject();
                handleMessage(msg);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setResponseProfile(ResponseProfile responseProfile) {
        this.responseProfile = responseProfile;
    }

    protected void handleMessage(Message msg) {
        // This is the default method to handle messages, we can override this in subclasses for specific behavior

        switch (responseProfile) {
            case IMMEDIATE:
                // Immediate response without delay
                processMessage(msg);
                break;
            case DELAY_SMALL:
                // Wait a small random time to simulate the delay in member response
                try {
                    Thread.sleep((long) (Math.random() * 500));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                processMessage(msg);
                break;
            case DELAY_LARGE:
                // Wait a large random time to simulate the delay in member response
                try {
                    Thread.sleep((long) (Math.random() * 2000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                processMessage(msg);
                break;
            case SLOW:
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
                processMessage(msg);
                break;
            case OFFLINE:
                // No response
                break;
            default:
                // Default behavior is to wait a small random time
                // Wait a small random time to simulate the delay in member response
                try {
                    Thread.sleep((long) (Math.random() * 500));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                processMessage(msg);
                break;
        }
    }

    protected void processMessage(Message msg) {
        System.out.println("Member " + id + " received message: " + msg);
        
        switch (msg.type) {
            case PREPARE:
                handlePrepare(msg);
                break;
            case PROMISE:
                handlePromise(msg);
                break;
            case ACCEPT_REQUEST:
                handleAcceptRequest(msg);
                break;
            case ACCEPTED:
                handleAccepted(msg);
                break;
            case LEARN:
                handleLearn(msg);
                break;
            default:
                System.out.println("Unknown message type received by member " + id);
                break;
        }
    }

    public void propose(String value) {
        this.proposalValue = value;
        this.proposalNumber = generateProposalNumber();
        promisesReceived.clear();
        highestAcceptedProposalNumbers.clear();
        previousAcceptedValues.clear();
        acceptRequestSent = false;
        acceptsReceived.clear();
        learnedValueSent = false;
        learnedValue = null;

        Message prepareMsg = new Message(Message.MessageType.PREPARE, id, proposalNumber, null);
        broadcastMessage(prepareMsg);
    }

    protected synchronized void handlePrepare(Message msg) {
        if (msg.proposalNumber > promisedProposalNumber) {
            promisedProposalNumber = msg.proposalNumber;
            Message promiseMsg = new Message(
                Message.MessageType.PROMISE,
                id,
                promisedProposalNumber,
                acceptedProposalNumber,
                acceptedValue
            );
            sendMessage(msg.senderId, promiseMsg);
        } else {
            // Ignore the Prepare message (already promised a higher proposal number)
        }
    }

    protected synchronized void handlePromise(Message msg) {
        if (msg.proposalNumber == proposalNumber) {
            promisesReceived.add(msg.senderId);
            if (msg.acceptedProposalNumber > -1 && msg.acceptedValue != null) {
                highestAcceptedProposalNumbers.put(msg.senderId, msg.acceptedProposalNumber);
                previousAcceptedValues.put(msg.senderId, msg.acceptedValue);
            }
            if (!acceptRequestSent && promisesReceived.size() > (memberIds.size() / 2)) {
                // Received promises from majority
                acceptRequestSent = true;
                String valueToPropose = proposalValue;
                // Choose the value with the highest accepted proposal number
                if (!previousAcceptedValues.isEmpty()) {
                    int highestProposalNum = -1;
                    for (Map.Entry<Integer, Integer> entry : highestAcceptedProposalNumbers.entrySet()) {
                        if (entry.getValue() > highestProposalNum) {
                            highestProposalNum = entry.getValue();
                            valueToPropose = previousAcceptedValues.get(entry.getKey());
                        }
                    }
                }
                Message acceptMsg = new Message(
                    Message.MessageType.ACCEPT_REQUEST,
                    id,
                    proposalNumber,
                    valueToPropose
                );
                broadcastMessage(acceptMsg);
            }
        }
    }

    protected synchronized void handleAcceptRequest(Message msg) {
        if (msg.proposalNumber >= promisedProposalNumber) {
            promisedProposalNumber = msg.proposalNumber;
            acceptedProposalNumber = msg.proposalNumber;
            acceptedValue = msg.value;
            Message acceptedMsg = new Message(
                Message.MessageType.ACCEPTED,
                id,
                acceptedProposalNumber,
                acceptedValue
            );
            sendMessage(msg.senderId, acceptedMsg);
        } else {
            // Ignore the Accept Request message (already promised a higher proposal number)
        }
    }

    protected synchronized void handleAccepted(Message msg) {
        if (msg.proposalNumber == proposalNumber) {
            acceptsReceived.add(msg.senderId);
            if (!learnedValueSent && acceptsReceived.size() > (memberIds.size() / 2)) {
                // Value is chosen
                learnedValue = msg.value;
                System.out.println("Member " + id + " learned value: " + learnedValue);
                // Notify all learners
                Message learnMsg = new Message(
                    Message.MessageType.LEARN,
                    id,
                    proposalNumber,
                    learnedValue
                );
                broadcastMessage(learnMsg);
                learnedValueSent = true;

                // stop the member since consensus is reached
                isRunning = false;
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected synchronized void handleLearn(Message msg) {
        if (learnedValue == null) {
            learnedValue = msg.value;
            System.out.println("Member " + id + " learned value: " + learnedValue);
            // stop the member if consensus is reached
            isRunning = false;
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void sendMessage(int recipientId, Message msg) {
        // Helper method to send a message to a specific member
        int recipientPort = memberPorts.get(recipientId);
        try (Socket socket = new Socket("localhost", recipientPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(msg);
        } catch (IOException e) {
            // Handle exception (recipient may be offline)
            System.out.println("Member " + id + " failed to send message to Member " + recipientId);
        }
    }

    protected void broadcastMessage(Message msg) {
        // Helper method to broadcast a message to all members except self
        for (int memberId : memberIds) {
            if (memberId != id) {
                sendMessage(memberId, msg);
            }
        }
    }

    protected synchronized int generateProposalNumber() {
        // Generate a unique proposal number
        proposalNumber += 1;
        return proposalNumber * 100 + id;
    }
}
