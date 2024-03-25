import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    public Client(Socket socket, String username){
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;
        }   catch (IOException e){
            closeEverything(socket,bufferedWriter,bufferedReader);
        }
    }

    public void sendMessage() {
        try {
            bufferedWriter.write(username); // Initially send the username to the server
            bufferedWriter.newLine();
            bufferedWriter.flush();

            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                String messageToSend = scanner.nextLine();

                // Check if the command to request member details is entered
                if (messageToSend.equalsIgnoreCase("/RMD")) {
                    messageToSend = "RMD|" + this.username; // Properly format the message for the server
                }
                // Check if the user wants to send a direct message
                else if (messageToSend.startsWith("/dm")) {
                    String[] parts = messageToSend.split(" ", 3);
                    if (parts.length >= 3) {
                        // Format the direct message correctly for the server to process
                        messageToSend = "DM|" + parts[1] + "|" + parts[2];
                    }
                }
                // If it's not a command, format it as a regular chat message
                else {
                    messageToSend = username + ": " + messageToSend;
                }

                bufferedWriter.write(messageToSend);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedWriter, bufferedReader);
        }
    }




    public  void listenForMessage(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;

                while (socket.isConnected()){
                    try {
                        msgFromGroupChat = bufferedReader.readLine();
                        System.out.println(msgFromGroupChat);
                    } catch (IOException e){
                        closeEverything(socket, bufferedWriter, bufferedReader);
                    }
                }
            }
        }).start();
    }

    public void closeEverything(Socket socket, BufferedWriter bufferedWriter, BufferedReader bufferedReader){
        try {
            if (bufferedReader != null){
                bufferedReader.close();
            }
            if (bufferedWriter != null){
                bufferedReader.close();
            }
            if (socket != null){
                socket.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your username for the group chat:");
        String username = scanner.nextLine();
        Socket socket = new Socket("localhost",12345);
        Client client = new Client(socket,username);
        client.listenForMessage();
        client.sendMessage();
    }
}