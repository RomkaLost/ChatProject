package client;

import com.google.gson.Gson;
import util.Message;
import util.TextMessage;
import util.UserInfoMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class ClientMessageListener implements Runnable {
    private Socket socket;
    private BlockingQueue queue;
    private Gson gson = new Gson();

    public ClientMessageListener(Socket clientSocket, BlockingQueue queue) {
        this.socket = clientSocket;
        this.queue = queue;
    }

    public void run() {
        try {
            while (true) {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //ждем сообщение от сервера, по типу решаем как его "разджейсонить"
                String jsonMessage = in.readLine();
                Message message = gson.fromJson(jsonMessage, Message.class);
                switch (message.getMessageType()) {
                    case TEXTFROMROOM:
                    case TEXTFROMSERVER:
                    case ERROR:
                    case ROOMNAMESET:
                        TextMessage textMessage = gson.fromJson(jsonMessage, TextMessage.class);
                        queue.put(textMessage);
                        break;
                    case NEWUSER:
                        UserInfoMessage userInfoMessage = gson.fromJson(jsonMessage, UserInfoMessage.class);
                        queue.put(userInfoMessage);
                        break;

                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

}