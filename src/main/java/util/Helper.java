package util;

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;

import static util.MessageType.ERROR;

public class Helper {
    Gson gson = new Gson();

    public void send(BufferedWriter out, Message message) throws IOException {
        String jsonMessage = gson.toJson(message);
        out.write(jsonMessage + "\n");
        out.flush();
    }


    public String readUserInput(String annotation) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(annotation);
        return reader.readLine();
    }

    public void error(Session session, String text) throws IOException {
        TextMessage errorMessage = new TextMessage();
        errorMessage.setText(text);
        errorMessage.setMessageType(ERROR);
        send(session.getOut(), errorMessage);
        System.out.println("сообщил об ощибке: " + text);

    }
}
