package CS6378P3;
import java.io.Serializable;

public class Message implements Comparable<Message>, Serializable {
    int from;
    MessageType messageType;
    String content;
    long T; 

    public Message(int from, MessageType messageType, String content) {
        this.from = from;
        this.messageType = messageType;
        this.content = content;
        this.T = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "T: "+  this.T +" from : " + from  + " messageType: " + messageType.toString() + " content : " + this.content;
    }

    @Override // just compare uids and let the priority queue ideally handle the rest, else we can use timestamps
    public int compareTo(Message other) {
        if (this.from > other.from) {
            return 1;
        } else {
            return 0;
        }
    }
}