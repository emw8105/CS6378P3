package CS6378P3.Client;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import CS6378P3.Commons.ConfigReader;
import CS6378P3.Commons.Node;

public class Main {
    // testing
    public static void spinClients(List<Node> serverList, List<Node> clientList) throws Exception {
        System.err.println("Spinning clients..");
        String hostname = InetAddress.getLocalHost().getHostName();
        List<Node> hostnodes = clientList.stream().filter(t -> t.hostname.equals(hostname)).toList();
        List<ClientProc> hostClientProcs = new ArrayList<ClientProc>();
        for (Node hn : hostnodes) {
            ClientProc cp = new ClientProc(hn.uid, hn.hostname, hn.port);
            for (Node n : serverList) {
                cp.addServer(n);
            }
            hostClientProcs.add(cp);
        }
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (ClientProc hcp : hostClientProcs) {
            executor.submit(() -> {
                for (int i = 0; i < 100; i++) {
                    try {
                        hcp.set(String.valueOf(i), String.valueOf((hcp.getuid() + 1) * i));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (ClientProc hcp : hostClientProcs) {
            for (int i = 0; i < 100; i++) {
                System.out.println(hcp.get(String.valueOf(i)));
            }
        }

    }

    public static void main(String[] args) throws IOException {
        String serversconfig = args[0];
        String clientsconfig = args[1];
        List<Node> serverNodes = ConfigReader.readConfigFile(serversconfig);
        List<Node> clientNodes = ConfigReader.readConfigFile(clientsconfig);
        try {
            spinClients(serverNodes, clientNodes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
