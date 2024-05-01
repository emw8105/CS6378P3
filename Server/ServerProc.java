package CS6378P3.Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import CS6378P3.Commons.Message;
import CS6378P3.Commons.Node;
import CS6378P3.Commons.MessageType;
import CS6378P3.Commons.DFSUtils;

//Synchronization
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerProc {
    private String hostname;
    private int uid;
    private int port;
    private Map<Integer, Node> serverMap;
    //private String replicaPath;
    private static final int NUM_SERVERS = 7;
    private static final int NUM_PARTITIONS = 8;
    private static final Boolean LOGGING = false;
    private String logfile;
    private String replicaFolder;

    private static ReentrantReadWriteLock[] partitionLocks = new ReentrantReadWriteLock[NUM_PARTITIONS];
    private String[] partitionPaths = new String[NUM_PARTITIONS];
    static {
        for (int i = 0; i < NUM_PARTITIONS; i++) {
            partitionLocks[i] = new ReentrantReadWriteLock();
        }
    }



    public ServerProc(int uid, String hostname, int port, String replicaFolder) {
        this.uid = uid;
        this.hostname = hostname;
        this.port = port;
        this.replicaFolder = replicaFolder;
        this.logfile = "logs/server"+uid+".txt";
        this.serverMap = new HashMap<>();// Initialize serverMap with empty HashMap
        this.setPartitionPaths();
    }

    public void addServer(Node node) {
        if (serverMap.size() < NUM_SERVERS) {
            serverMap.put(node.uid, node);
        }
    }

    private void logger(String logString) throws IOException{
        if(LOGGING){
            PrintWriter writer = new PrintWriter(new FileWriter(this.logfile, true));
            writer.println(logString);
            writer.close();
        }
    }

    private Node hash(String key, int offset) {
        int serverNum = (Math.abs(key.hashCode()) + offset) % NUM_SERVERS; // need to change hash function to correlate
                                                                          // the object to the server it belongs to
        Node server = serverMap.get(serverNum);
        if (server == null) {
            throw new RuntimeException("No server found for key: " + key);
        }
        return server;
    }

    private void setPartitionPaths(){
        for (int i = 0; i < NUM_PARTITIONS; i++) {
            partitionPaths[i] = this.replicaFolder + "/server_" + uid +"/part"+i+".dat";
        }
    }

    private void setupFileSystem() throws Exception{
        try{
            DFSUtils.createFile(this.logfile);
            for (int i = 0; i < NUM_PARTITIONS; i++) {
                 DFSUtils.createFile(getPartitionFilepath(i));
                 Map<String, String> keyValueMap = new HashMap<>();
                 writeMap(keyValueMap, getPartitionFilepath(i));
            }
        }
        catch (Exception e){
            throw e;
        }   
    }

    private String getPartitionFilepath(int partitionID){
        return partitionPaths[partitionID];
    }

    private int getPartitionID(String key){
        return Math.abs(key.hashCode())%NUM_PARTITIONS;
    }


    private Map<String, String> readMap(String replicapath) throws IOException , ClassNotFoundException {
       try(ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(replicapath))){
        Object obj = inputStream.readObject();
        @SuppressWarnings("unchecked")
        Map<String, String> keyValueMap = (Map<String, String>) obj;
        inputStream.close();
        return keyValueMap;
       }catch (IOException | ClassNotFoundException e) {
        throw e;
       }
    } 
    private void writeMap(Map<String, String> keyValueMap,String replicapath) throws IOException{
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(replicapath))) {
            outputStream.writeObject(keyValueMap);
            outputStream.close();
        } catch (IOException e) {
            throw e;
        }
    }

    //assumes partitionID is valid
    private Map<String, String> readPartitionByID(int partitionID) throws IOException , ClassNotFoundException {
        String partitionPath = getPartitionFilepath(partitionID);
        partitionLocks[partitionID].readLock().lock();
        try{
            Map<String, String> keyValueMap = readMap(partitionPath);
            return keyValueMap;
        }catch (IOException e) {
            throw e;
        }finally{
            partitionLocks[partitionID].readLock().unlock();
        }
    }

    //assumes partitionID is valid
    private void writePartitionByID(int partitionID,Map<String, String> keyValueMap) throws IOException , ClassNotFoundException {
        String partitionPath = getPartitionFilepath(partitionID);
        partitionLocks[partitionID].writeLock().lock();
        try{
            writeMap(keyValueMap,partitionPath);
        }catch (IOException e) {
            throw e;
        }finally{
            partitionLocks[partitionID].writeLock().unlock();
        }
    }

    

    private Map<String, String> readPartitionByKey(String key) throws IOException , ClassNotFoundException {
        int partitionID = getPartitionID(key);
        return readPartitionByID(partitionID);
    }

    private void writePartitionByKey(String key, Map<String, String> keyValueMap) throws IOException, ClassNotFoundException{
        int partitionID = getPartitionID(key);
        writePartitionByID(partitionID,keyValueMap);
    }



    public String getKV(String key) {
        String value = null;
        try{
            Map<String, String> keyValueMap = readPartitionByKey(key);//readMap(replicaPath);
            value = keyValueMap.get(key);
        }catch(IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
        return value;
    }

    public Boolean setKV(String key, String value) {
          try{
            Map<String, String> keyValueMap =  readPartitionByKey(key);//readMap(replicaPath);
            keyValueMap.put(key, value);
            writePartitionByKey(key,keyValueMap);
            //writeMap(keyValueMap,replicaPath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void handle_local_read(Message inMessage, ObjectOutputStream objectOut) throws IOException {
        logger(inMessage.toString());
        String value = this.getKV(inMessage.key);
        if (value != null) {
            objectOut.writeObject(new Message(uid, MessageType.POS_ACK, inMessage.key, value));
        } else {
            objectOut.writeObject(new Message(uid, MessageType.NEG_ACK, inMessage.key, null));
        }
    }

    // use 2 phase commit
    public void handle_cs_write(Message inMessage, ObjectOutputStream objectOut)
            throws IOException, ClassNotFoundException {
        // send write_prepare to 2 servers,
        logger(inMessage.toString());
        Message prepMessage = new Message(uid, MessageType.SS_WRITE_PREPARE, inMessage.key, inMessage.value);
        Node server1 = hash(inMessage.key, 2);
        Node server2 = hash(inMessage.key, 4);
        Socket server_socket1 = new Socket(server1.hostname, server1.port);
        Socket server_socket2 = new Socket(server2.hostname, server2.port);

        ObjectOutputStream s1_outputstream = new ObjectOutputStream(server_socket1.getOutputStream());
        ObjectOutputStream s2_outputStream = new ObjectOutputStream(server_socket2.getOutputStream());

        ObjectInputStream s1_inputstream = new ObjectInputStream(server_socket1.getInputStream());
        ObjectInputStream s2_inputstream = new ObjectInputStream(server_socket2.getInputStream());

        s1_outputstream.writeObject(prepMessage);
        s2_outputStream.writeObject(prepMessage);
        logger(prepMessage.toString());

        Message s1_reply = (Message) s1_inputstream.readObject();
        Message s2_reply = (Message) s2_inputstream.readObject();

        Boolean commit = (s1_reply.messageType == MessageType.POS_ACK || s2_reply.messageType == MessageType.POS_ACK);
        Message secondPhaseMessage;
        if (commit) {
            secondPhaseMessage = new Message(uid, MessageType.SS_WRITE_COMMIT, inMessage.key, inMessage.value);
        } else {
            secondPhaseMessage = new Message(uid, MessageType.SS_WRITE_ABORT, inMessage.key, inMessage.value);
        }
        s1_outputstream.writeObject(secondPhaseMessage);
        s2_outputStream.writeObject(secondPhaseMessage);
        logger(secondPhaseMessage.toString());
        s1_inputstream.readObject();
        s2_inputstream.readObject();

        Boolean localSet = setKV(inMessage.key, inMessage.value);
        MessageType responseType;
        if ((localSet && commit)
                || (s1_reply.messageType == MessageType.POS_ACK && s2_reply.messageType == MessageType.POS_ACK)) {
            responseType = MessageType.POS_ACK;
        } else {
            responseType = MessageType.NEG_ACK;
        }
        Message completionMessage = new Message(uid, responseType, inMessage.key, inMessage.value);
        objectOut.writeObject(completionMessage);
        logger(completionMessage.toString());
        server_socket1.close();
        server_socket2.close();
    }

    public void handle_ss_write_commit(Message inMessage, ObjectOutputStream objectOut) throws IOException {
        Boolean writeSuccess = setKV(inMessage.key, inMessage.value);
        logger(inMessage.toString());
        if (writeSuccess) {
            objectOut.writeObject(new Message(uid, MessageType.POS_ACK, inMessage.key, inMessage.value));
        } else {
            objectOut.writeObject(new Message(uid, MessageType.NEG_ACK, inMessage.key, null));
        }
    }

    public void handle_ss_write_prepare(Message inMessage,ObjectInputStream objectIn, ObjectOutputStream objectOut)
            throws IOException, ClassNotFoundException {
        // send prepare ack  
        Message prepareAck = new Message(uid,MessageType.POS_ACK,inMessage.key, inMessage.value);
        objectOut.writeObject(prepareAck);
        Message commitOrAbort = (Message) objectIn.readObject(); // blocks until commit or abbort comes from requesting
                                                                 // server
        logger(commitOrAbort.toString());                                                   
        if (commitOrAbort.messageType == MessageType.SS_WRITE_COMMIT) {
            handle_ss_write_commit(inMessage, objectOut);
        } else {
            objectOut.writeObject(new Message(uid, MessageType.POS_ACK, inMessage.key, inMessage.value));
        }
    }

    public void handle_cs_print(ObjectOutputStream objectOut) {
        MessageType responseType = MessageType.POS_ACK;
        try{
            for(int partID = 0 ; partID < NUM_PARTITIONS ; partID++){
                Map <String , String> keyValueMap = readPartitionByID(partID);
                for (Map.Entry<String, String> entry : keyValueMap.entrySet()) {
                    System.out.println(entry.getKey() + " : " + entry.getValue());
                }
            }  
        }
        catch (IOException | ClassNotFoundException e) {
            responseType = MessageType.NEG_ACK;
            e.printStackTrace();
        }finally{
            try{
                objectOut.writeObject(new Message(uid, responseType, "", ""));
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // this method gets invoked only for the new request (old regests get handled
    // through read write streams )
    public void handleRequest(Socket connectionReq) {
        try {
            ObjectOutputStream objectOut = new ObjectOutputStream(connectionReq.getOutputStream());
            ObjectInputStream objectIn = new ObjectInputStream(connectionReq.getInputStream());
            Message inMessage = (Message) objectIn.readObject(); // blocks until there is a new message
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
                    handle_ss_write_prepare(inMessage,objectIn, objectOut);
                    break;
                case CS_PRINT:
                    handle_cs_print(objectOut);
                    break;
                case SS_WRITE_COMMIT:
                    System.out.print("should not reach here");
                    break;
                case SS_WRITE_ABORT:
                    // should not reach here
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
        } finally {
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

    public Thread run() throws Exception {
        setupFileSystem();
        Thread receiverThread = new Thread(() -> {
            try {
                this.startListening();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        receiverThread.start();
        return receiverThread;
    }
}
