import java.io.Serializable;

public class Message implements Serializable {
    public enum MessageType {
        PREPARE,
        PROMISE,
        ACCEPT_REQUEST,
        ACCEPTED,
        LEARN
    }

    public MessageType type;
    public int senderId;
    public int proposalNumber;
    public String value;
    public int acceptedProposalNumber;
    public String acceptedValue;

    public Message(MessageType type, int senderId, int proposalNumber, String value) {
        this.type = type;
        this.senderId = senderId;
        this.proposalNumber = proposalNumber;
        this.value = value;
        this.acceptedProposalNumber = -1;
        this.acceptedValue = null;
    }

    // Overloaded constructor for Promise messages
    public Message(MessageType type, int senderId, int proposalNumber, int acceptedProposalNumber, String acceptedValue) {
        this.type = type;
        this.senderId = senderId;
        this.proposalNumber = proposalNumber;
        this.value = null;
        this.acceptedProposalNumber = acceptedProposalNumber;
        this.acceptedValue = acceptedValue;
    }

    public String toString() {
        return "Message{" +
                "type=" + type +
                ", senderId=" + senderId +
                ", proposalNumber=" + proposalNumber +
                ", value='" + value + '\'' +
                ", acceptedProposalNumber=" + acceptedProposalNumber +
                ", acceptedValue='" + acceptedValue + '\'' +
                '}';
    }
}
