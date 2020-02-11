package util;

import java.io.*;
import java.net.Socket;
import java.sql.PreparedStatement;

public class Session {

    private Socket socket;

    private User user;
    private BufferedWriter out;
    private BufferedReader in;



    public Session(Socket socket) {

        try {
            this.socket = socket;
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public BufferedWriter getOut() {
        return out;
    }

    public BufferedReader getIn() {
        return in;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

}
