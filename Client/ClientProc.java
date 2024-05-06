package CS6378P3.Client;
// Need to implement contact list which will contain values corresponding to server connections
/* servers.txt:

# UID Hostname Port
0 dc01.utdallas.edu 5001
1 dc02.utdallas.edu 5001
2 dc03.utdallas.edu 5001
3 dc04.utdallas.edu 5001
4 dc05.utdallas.edu 5001
5 dc06.utdallas.edu 5001
6 dc07.utdallas.edu 5001
#

*/

// Should be good:
// functionalize the connection part of each request method
// use an object for AdjNodes (ServerNodes) instead of strings in the hashmap
// messages should contain the client id for total ordering among servers (Message class)

// Need to do:
// split file parsing responsibility and abstract it from servers/files to protect directory changes
// reading the config should be a separate function, use main to create a ist of clients?

import java.io.*;
import java.net.*;
import java.util.*;

import CS6378P3.Commons.Message;
import CS6378P3.Commons.Node;
import CS6378P3.Commons.MessageType;
import CS6378P3.Commons.DFSUtils;

public class ClientProc {
    private int uid;
    private String hostname;
    private int port;
    private Map<Integer, Node> serverMap = new HashMap<>();;
    private final int NUM_SERVERS = DFSUtils.NUM_SERVERS;
    private static final Boolean LOGGING = false;
    private String logfile;

    public ClientProc(int uid, String hostname, int port) {
        this.uid = uid;
        this.hostname = hostname;
        this.port = port;
        this.logfile  = "logs/client"+uid+".txt";
        DFSUtils.createFile(this.logfile);
    }
    private void logger(String logString) throws IOException{
        if(LOGGING){
            PrintWriter writer = new PrintWriter(new FileWriter(this.logfile, true));
            writer.println(logString);
            writer.close();
        }
    }

    public static Map<Integer, Node> readServerDetailsFromFile(String filename) throws FileNotFoundException {
        Map<Integer, Node> serverMap = new HashMap<>();
        Scanner scanner = new Scanner(new File(filename));
        while (scanner.hasNextLine()) {
            String[] details = scanner.nextLine().split(" "); // format: serverNumber hostname port
            int serverNumber = Integer.parseInt(details[0]);
            String hostname = details[1];
            int port = Integer.parseInt(details[2]);
            serverMap.put(serverNumber, new Node(serverNumber, hostname, port));
        }
        scanner.close();
        return serverMap;
    }

    public void addServer(Node node) {
        if ( serverMap.size() < NUM_SERVERS ){
            serverMap.put(node.uid, node);
        }
    }
    public int getuid(){
        int r = this.uid;
        return r;
    }

    // hash the key to determine which server to connect to
    private Node hash(String key, int offset) {
        int serverNum = (Math.abs(key.hashCode()) + offset) % NUM_SERVERS; // need to change hash function to correlate the object to the server it belongs to
        Node server = serverMap.get(serverNum);
        if (server == null) {
            throw new RuntimeException("No server found for key: " + key);
        }
        return server;
    }

    // connect to the server that the key hashes to
    private Socket connect(Node server) throws IOException {
        //System.out.println("Attempting to connect to " + server.hostname + ":" + server.port);
        Socket socket = new Socket(server.hostname, server.port);
        //socket.setSoTimeout(10000); // set a timeout of 1 second
        //System.out.println("Connection Established");
        return socket;
    }

    private ObjectOutputStream getWriter(Socket socket) throws IOException {
        return new ObjectOutputStream(socket.getOutputStream());
    }
    
    private ObjectInputStream getReader(Socket socket) throws IOException {
        return new ObjectInputStream(socket.getInputStream());
    }

    public Message get(String key) throws IOException, ClassNotFoundException {
        Message response = null;
        int replica_try = 0;
        while (replica_try < 3) {
            try {
                Node destination = hash(key, replica_try*2); // hash the key to determine which server to connect to
                Socket socket = connect(destination); // connect to the server that the key hashes to
                ObjectOutputStream out = getWriter(socket);
                ObjectInputStream in = getReader(socket);
                Message message = new Message(uid, MessageType.CS_READ, key,"");
                out.writeObject(message);
                response = (Message)in.readObject(); // blocks until it receives a response or the timeout expires
                socket.close();
                if (response != null && response.messageType == MessageType.POS_ACK) {
                    break; 
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            replica_try += 1; // move to the next server in the hash sequence
        }
        if (response == null) {
            throw new RuntimeException("Unable to read the key: " + key);
        }
        return response;
    }

    public MessageType set(String key, String value) throws IOException, ClassNotFoundException {
        Node destination = hash(key, 0); // hash the key to determine which server to connect to
        Socket socket = connect(destination); // connect to the server that the key hashes to
        ObjectOutputStream out = getWriter(socket);
        ObjectInputStream in  = getReader(socket);
        Message message = new Message(uid, MessageType.CS_WRITE, key,value);
        out.writeObject(message);
        logger(message.toString());
        Message response = (Message)in.readObject(); // blocks until it receives a response
        logger(response.toString());
        socket.close();
        return response.messageType;
    }

}