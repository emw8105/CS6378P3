package CS6378P3.Commons;
import java.io.Serializable;

public class Message implements Comparable<Message>, Serializable {
    public int from;
    public MessageType messageType;
    public String key;
    public String value;
    public long T; 

    public Message(int from, MessageType messageType, String key, String value) {
        this.from = from;
        this.messageType = messageType;
        this.key = key;
        this.value = value;
        this.T = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "T: "+  this.T +" from : " + from  + " messageType: " + messageType.toString() + " content : " + this.key + " " + this.value;
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