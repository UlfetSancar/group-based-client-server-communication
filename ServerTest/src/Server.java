import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private ServerSocket serverSocket; // listening incoming clients and creating socket object to communicate

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void startServer(){ //keep the server running

        try { // input output error handling

            while (!serverSocket.isClosed()) { //server run indefinitely

                Socket socket = serverSocket.accept(); // waiting for client connection
                System.out.println("A new Client has connected");
                ClientHandler clientHandler = new ClientHandler(socket); // responsble for communicating with a client and interface runnable

                Thread thread = new Thread(clientHandler); //impement runable
                thread.start();

            }
        }  catch (IOException e){ // input output exceptions

        }

    }

    public void closeServerSocket() { //error handling
        try {
            if (serverSocket != null){
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException { //instantiate an object from server class and run
        ServerSocket serverSocket = new ServerSocket(12345); // listening for clients that are making a connection
        Server server = new Server(serverSocket);
        server.startServer();
    }
}