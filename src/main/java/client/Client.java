package client;

import com.google.gson.Gson;
import util.Helper;
import util.Message;
import util.TextMessage;
import util.User;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static util.MessageType.TEXTFROMUSER;

public class Client {
    private UUID uuid = UUID.randomUUID();
    private static Socket clientSocket;
    private static final int PORT = 5555;
    private MyInfo myInfo = MyInfo.getInstance();
    private static BlockingQueue<Message> queue = new ArrayBlockingQueue<>(5);
    private HashMap<Integer, User> userMap= new HashMap<>();
    private Helper helper = new Helper();

    public static void main(String[] args) {
        Client client = new Client(PORT);
    }

    public Client(int PORT) {
        try {
            clientSocket = new Socket("localhost", PORT);
            ClientAuthenticate clientAuthenticate = new ClientAuthenticate();
            ExecutorService executor = Executors.newCachedThreadPool();
            clientAuthenticate.auth(clientSocket, myInfo, userMap);
            executor.submit(new ClientMessageListener(clientSocket, queue));
            executor.submit(new ClientMessageWorker(clientSocket, myInfo, queue, userMap));
            readFromKeyboard();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                clientSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }


    }

    public void readFromKeyboard() throws IOException {
        while (true) {

            TextMessage textMessage = new TextMessage();
            textMessage.setMessageType(TEXTFROMUSER);
            textMessage.setText(helper.readUserInput("Введите сообщение"));
            queue.add(textMessage);
        }
    }

}
