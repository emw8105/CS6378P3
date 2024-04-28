package CS6378P3.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class RequestHandler implements Runnable {
    private Socket clientSocket;
    public RequestHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
    }

    @Override
        public void run() {
            try {
                // Create input and output streams for communication
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Read message from client
                String message = in.readLine();
                System.out.println("Received message from client: " + message);

                // Process message (here you can implement your own logic)

                // Send response back to client
                out.println("Response from server: Message received!");

                // Close the connection
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


}
