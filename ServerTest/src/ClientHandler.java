import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
public class ClientHandler implements Runnable{

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();  // keep track of all clients
    private static ClientHandler coordinator = null; // Keep track of the coordinator
    private Socket socket; // establish a connection between the client and server
    private BufferedReader bufferedReader; // read data, messages that have been sent from the client
    private BufferedWriter bufferedWriter; // sand data, messages to client, from other clients
    private String clientUsername; // represent each client

    public ClientHandler(Socket socket){ //constructor, instances of this class passed a socket object from server class
        try { // class properties
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); // send message
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream())); //receive message
            this.clientUsername = bufferedReader.readLine();
            clientHandlers.add(this); // add clint to arraylist

            // Check if this is the first client (coordinator)
            if (coordinator == null) {
                coordinator = this; // Set the current client as the coordinator
                // Inform the coordinator
                this.bufferedWriter.write("You are the coordinator.");
                this.bufferedWriter.newLine();
                this.bufferedWriter.flush();
            } else {
                // Inform the new client about the coordinator
                this.bufferedWriter.write("The current coordinator is: " + coordinator.clientUsername);
                this.bufferedWriter.newLine();
                this.bufferedWriter.flush();
            }

            broadcastMessage("SERVER:" + clientUsername + " has entered the chat");
        } catch (IOException e){
            closeEverything(socket, bufferedWriter, bufferedReader);
        }
    }

    private void sendMemberDetails(String requesterUsername) { // Method to handle requests for member details
        System.out.println("Sending member details to: " + requesterUsername); // Debugging log
        if (this == coordinator) { // Ensure this ClientHandler is the coordinator
            StringBuilder memberDetails = new StringBuilder("Member details:\n");
            for (ClientHandler clientHandler : clientHandlers) {
                Socket memberSocket = clientHandler.socket;
                memberDetails.append("Username: ").append(clientHandler.clientUsername)
                        .append(", IP: ").append(memberSocket.getInetAddress().getHostAddress())
                        .append(", Port: ").append(memberSocket.getPort())
                        .append(clientHandler == coordinator ? " (Coordinator)" : "")
                        .append("\n");
            }
            try {
                for (ClientHandler clientHandler : clientHandlers) {
                    if (clientHandler.clientUsername.equals(requesterUsername)) {
                        clientHandler.bufferedWriter.write(memberDetails.toString());
                        clientHandler.bufferedWriter.newLine();
                        clientHandler.bufferedWriter.flush();
                        break; // Stop after sending to the requester
                    }
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedWriter, bufferedReader);
            }
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                if (messageFromClient.startsWith("DM|")) {
                    handleDirectMessage(messageFromClient);
                } else if (messageFromClient.startsWith("RMD|")) { // Updated to recognize "RMD"(REQUEST_MEMBER_DETAILS)
                    System.out.println("Received request for member details: " + messageFromClient); // Debugging log
                    sendMemberDetails(messageFromClient.split("\\|")[1]); // Extract the username and process the request
                } else {
                    broadcastMessage(messageFromClient);
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedWriter, bufferedReader);
                break;
            }
        }
    }


    public void broadcastMessage(String messageToSend) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedWriter, bufferedReader);
            }
        }
    }

    private void handleDirectMessage(String message) {
        String[] parts = message.split("\\|", 3);
        if (parts.length == 3) {
            String recipientUsername = parts[1];
            String messageToSend = parts[2];
            sendDirectMessage(recipientUsername, "DM from " + this.clientUsername + ": " + messageToSend);
        }
    }


    private void sendDirectMessage(String recipientUsername, String messageToSend) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (clientHandler.clientUsername.equals(recipientUsername)) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedWriter, bufferedReader);
            }
        }
    }



    public void closeEverything(Socket socket, BufferedWriter bufferedWriter, BufferedReader bufferedReader) {
        removeClientHandler(); // Call this at the beginning to ensure the client is removed first
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage("SERVER: " + clientUsername + " has left the chat.");
        if (this == coordinator && !clientHandlers.isEmpty()) { // Only reassign coordinator if necessary
            coordinator = clientHandlers.get(0); // Assign new coordinator
            try {
                coordinator.bufferedWriter.write("You are the new coordinator.");
                coordinator.bufferedWriter.newLine();
                coordinator.bufferedWriter.flush();
                broadcastMessage("SERVER: New coordinator is " + coordinator.clientUsername);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (clientHandlers.isEmpty()) {
            coordinator = null; // Reset coordinator if no clients are connected
        }
    }

}
