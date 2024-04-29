package CS6378P3.Commons;
public class Node {
    public int uid;
    public String hostname;
    public int port;
    public Node(int uid, String hostname, int port){
        this.uid = uid;
        this.hostname = hostname;
        this.port = port;
    }
}