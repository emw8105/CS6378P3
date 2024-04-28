package CS6378P3.Server;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import CS6378P3.Commons.Message;
import CS6378P3.Commons.Node;
import CS6378P3.Commons.MessageType;

public class ServerProc {
    private String hostname;
    private int uid;
    private int port;
    private Map<Integer, Node> serverMap;
    private String replicaPath;
    private final int numServers = 7;
    public ServerProc(int uid, String hostname, int port, String replicaFolder) {
        this.uid = uid;
        this.hostname = hostname;
        this.port = port;
        this.replicaPath = replicaFolder + "/server_" + uid + ".dat"; 
        // Initialize serverMap with empty HashMap
        this.serverMap = new HashMap<>();
    }

    public void addServer(int serverId, Node node) {
        if ( serverMap.size() < numServers ){
            serverMap.put(serverId, node);
        }
    }


    private Node hash(String key, int offset) {
        int serverNum = Math.abs(key.hashCode()) % numServers; // need to change hash function to correlate the object to the server it belongs to
        Node server = serverMap.get(serverNum);
        if (server == null) {
            throw new RuntimeException("No server found for key: " + key);
        }
        return server;
    }

    public String getKV(String key) {
        String value = null;
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(replicaPath))) {
            // Read the object from the file
            Object obj = inputStream.readObject();
            if (obj instanceof Map) {
                // Cast the object to a Map<String, String> if it's of the correct type
                @SuppressWarnings("unchecked")
                Map<String, String> keyValueMap = (Map<String, String>) obj;
                value = keyValueMap.get(key);
            } else {
                System.err.println("Error: The object in the file is not a Map<String, String>.");
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return value;
    }

    public Boolean setKV(String key, String value) {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(replicaPath))) {
            @SuppressWarnings("unchecked")
            Map<String, String> keyValueMap = (Map<String, String>) inputStream.readObject();
            keyValueMap.put(key, value);
            inputStream.close();
            try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(replicaPath))) {
                outputStream.writeObject(keyValueMap);
                outputStream.close();
                return true; // Return true to indicate success
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void handle_local_read(Message inMessage, ObjectOutputStream objectOut) throws IOException {
        String value = this.getKV(inMessage.key);
        if (value != null) {
            objectOut.writeObject(new Message(uid, MessageType.POS_ACK, inMessage.key, value));
        } else {
            objectOut.writeObject(new Message(uid, MessageType.NEG_ACK, inMessage.key, null));
        }
    }

    

    // use 2 phase commit
    public void handle_cs_write(Message inMessage, ObjectOutputStream objectOut) throws IOException, ClassNotFoundException {
        // send write_prepare to 2 servers,
        Message prepMessage = new Message(uid,MessageType.SS_WRITE_PREPARE,inMessage.key,inMessage.value);
        Node server1 = hash(inMessage.key,2);
        Node server2 = hash(inMessage.key,4);
        Socket server_socket1 = new Socket(server1.hostname, server1.port);
        Socket server_socket2 = new Socket(server2.hostname, server2.port);

        ObjectOutputStream s1_outputstream = new ObjectOutputStream(server_socket1.getOutputStream());
        ObjectOutputStream s2_outputStream = new ObjectOutputStream(server_socket2.getOutputStream());

        ObjectInputStream s1_inputstream = new ObjectInputStream(server_socket1.getInputStream());
        ObjectInputStream s2_inputstream = new ObjectInputStream(server_socket2.getInputStream());

        s1_outputstream.writeObject(prepMessage);
        s2_outputStream.writeObject(prepMessage);

        Message s1_reply = (Message) s1_inputstream.readObject(); 
        Message s2_reply = (Message) s2_inputstream.readObject(); 

        Boolean commit = (s1_reply.messageType == MessageType.POS_ACK || s2_reply.messageType == MessageType.POS_ACK) ;
        Message secondPhasMessage;
        if(commit){
            secondPhasMessage = new Message(uid, MessageType.SS_WRITE_COMMIT, inMessage.key, inMessage.value);
        }else{
            secondPhasMessage = new Message(uid, MessageType.SS_WRITE_ABORT, inMessage.key, inMessage.value);
        }
        s1_outputstream.writeObject(secondPhasMessage);
        s2_outputStream.writeObject(secondPhasMessage);
        s1_inputstream.readObject(); 
        s2_inputstream.readObject(); 

        Boolean localSet = setKV(inMessage.key, inMessage.value);
        MessageType responseType;
        if((localSet && commit) || (s1_reply.messageType == MessageType.POS_ACK && s2_reply.messageType == MessageType.POS_ACK)){
            responseType = MessageType.POS_ACK;
        }else{
            responseType = MessageType.NEG_ACK;
        }
        objectOut.writeObject(new Message(uid, responseType, replicaPath, hostname));
        server_socket1.close();
        server_socket2.close();
    }

    
    public void handle_ss_write_commit(Message inMessage, ObjectOutputStream objectOut) throws IOException {
        Boolean writeSuccess = setKV(inMessage.key, inMessage.value);
        if (writeSuccess) {
            objectOut.writeObject(new Message(uid, MessageType.POS_ACK, inMessage.key, inMessage.value));
        } else {
            objectOut.writeObject(new Message(uid, MessageType.NEG_ACK, inMessage.key, null));
        }
    }

    public void handle_ss_write_prepare(Message inMessage, ObjectInputStream objectIn, ObjectOutputStream objectOut) throws IOException, ClassNotFoundException {
        Message commitOrAbort = (Message) objectIn.readObject(); // blocks until commit or abbort comes from requesting server
        if (commitOrAbort.messageType == MessageType.SS_WRITE_COMMIT) {
            handle_ss_write_commit(inMessage,objectOut);
        } else {
            objectOut.writeObject(new Message(uid, MessageType.POS_ACK, inMessage.key, inMessage.value));
        }
    }

    

    // this method gets invoked only for the new request (old regests get handled through read write streams )
    public void handleRequest(Socket connectionReq) {
        try {
            ObjectOutputStream objectOut = new ObjectOutputStream(connectionReq.getOutputStream());
            ObjectInputStream objectIn = new ObjectInputStream(connectionReq.getInputStream());
            Message inMessage = (Message) objectIn.readObject();
            switch (inMessage.messageType) {
                case CS_READ:
                    handle_local_read(inMessage, objectOut);
                    break;
                case CS_WRITE:
                    handle_cs_write(inMessage, objectOut);
                    break;
                case SS_READ:
                    handle_local_read(inMessage, objectOut);
                    break;
                case SS_WRITE_PREPARE:
                    handle_ss_write_prepare(inMessage,objectIn,objectOut);
                    break;
                case SS_WRITE_COMMIT:
                    System.out.print("should not reach here");
                    break;
                case SS_WRITE_ABORT:
                    //should not reach here
                    System.out.print("should not reach here");
                    break;
                case ERROR:
                    // should not reach here
                    System.out.print("should not reach here");
                    break;
                case POS_ACK:
                    // should not reach here
                    System.out.print("should not reach here");
                    break;
                case NEG_ACK:
                    // should not reach here
                    System.out.print("should not reach here");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                connectionReq.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void startListening() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            while (true) {
                Socket connectionReq = serverSocket.accept();
                executor.execute(() -> {
                    try {
                        handleRequest(connectionReq);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            connectionReq.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

        } finally {
            serverSocket.close();
            executor.shutdown();
        }
    }
}
