package CS6378P3;
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

public class Client {
    private int uid;
    private String hostname;
    private int port;
    private Map<Integer, ServerNode> serverMap;
    private final int numServers = 7;
    private final String serverContactList = "servers.txt";

    public Client(int uid, String hostname, int port) {
        this.uid = uid;
        this.hostname = hostname;
        this.port = port;
        try {
            // this.hostname = InetAddress.getLocalHost().getHostName(); // hostname may be passed in as an argument instead
            this.serverMap = readServerDetailsFromFile(serverContactList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<Integer, ServerNode> readServerDetailsFromFile(String filename) throws FileNotFoundException {
        Map<Integer, ServerNode> serverMap = new HashMap<>();
        Scanner scanner = new Scanner(new File(filename));
        while (scanner.hasNextLine()) {
            String[] details = scanner.nextLine().split(" "); // format: serverNumber hostname port
            int serverNumber = Integer.parseInt(details[0]);
            String hostname = details[1];
            int port = Integer.parseInt(details[2]);
            serverMap.put(serverNumber, new ServerNode(serverNumber, hostname, port));
        }
        scanner.close();
        return serverMap;
    }

    // hash the key to determine which server to connect to
    private ServerNode hash(String key, int offset) {
        int serverNum = Math.abs(key.hashCode()) % numServers; // need to change hash function to correlate the object to the server it belongs to
        ServerNode server = serverMap.get(serverNum);
        if (server == null) {
            throw new RuntimeException("No server found for key: " + key);
        }
        return server;
    }

    // connect to the server that the key hashes to
    private Socket connect(ServerNode server) throws IOException {
        System.out.println("Attempting to connect to " + server.hostname + ":" + server.port);
        Socket socket = new Socket(server.hostname, server.port);
        socket.setSoTimeout(1000); // set a timeout of 1 second
        System.out.println("Connection Established");
        return socket;
    }

    private ObjectOutputStream getWriter(Socket socket) throws IOException {
        return new ObjectOutputStream(socket.getOutputStream());
    }
    
    private BufferedReader getReader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public String sendReadRequest(Socket socket, String key) throws IOException {
        String response = null;
        int offset = 0;
        while (offset < numServers) {
            try {
                ServerNode destination = hash(key, offset); // hash the key to determine which server to connect to
                connect(destination); // connect to the server that the key hashes to
                ObjectOutputStream out = getWriter(socket);
                BufferedReader in = getReader(socket);
                Message message = new Message(uid, MessageType.READ, key);
                out.writeObject(message);
                response = in.readLine(); // blocks until it receives a response or the timeout expires
                socket.close();
                if (response != null && !response.contains("ERROR")) {
                    break; // if the response is not null and does not contain "ERROR", we have a successful response
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            offset += 2; // move to the next server in the hash sequence
        }
        if (response == null || response.contains("ERROR")) {
            throw new RuntimeException("Unable to read the key: " + key);
        }
        return response;
    }

    public String sendWriteRequest(Socket socket, String key, String value) throws IOException {
        ServerNode destination = hash(key, 0); // hash the key to determine which server to connect to
        connect(destination); // connect to the server that the key hashes to
        ObjectOutputStream out = getWriter(socket);
        BufferedReader in = getReader(socket);
        Message message = new Message(uid, MessageType.WRITE, key + " " + value); // currently concatenating key and value
        out.writeObject(message);
        String response = in.readLine(); // blocks until it receives a response
        socket.close();
        return response;
    }
}