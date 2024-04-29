package CS6378P3.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import CS6378P3.Commons.ConfigReader;
import CS6378P3.Commons.Node;

public class Main {

    public static List<Thread> spinHostServers(List<Node> nodes) throws Exception {
        String hostname = InetAddress.getLocalHost().getHostName();
        List<Node> hostnodes = nodes.stream().filter(t -> t.hostname.equals(hostname)).toList();
        List<ServerProc> hostSeverProcs = new ArrayList<ServerProc>();
        String storageFolder = "/Storage";
        for (Node hn : hostnodes) {
            ServerProc sp = new ServerProc(hn.uid, hn.hostname, hn.port, storageFolder);
            for (Node n : nodes) {
                sp.addServer(n);
            }
            hostSeverProcs.add(sp);
        }
        List<Thread> returnVal = new ArrayList<Thread>();
        for (ServerProc hp : hostSeverProcs) {
            returnVal.add(hp.run());
        }
        return returnVal;
    }

    public static void main(String[] args) throws IOException {
        String filePath = args[0];
        List<Node> nodes = ConfigReader.readConfigFile(filePath);
        try {
            List<Thread> serverThreads = spinHostServers(nodes);
            Thread.sleep(10 * 60 * 1000); // 10 minutes in milliseconds
            for (Thread thread : serverThreads) {
                thread.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
