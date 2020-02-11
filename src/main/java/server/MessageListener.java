package server;

import com.google.gson.Gson;
import util.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MessageListener implements Runnable {
    private Session session;
    Gson gson = new Gson();
    Future<UUID> uuidFuture;
    private HashMap<UUID, Session> sessionMap;
    private BlockingQueue queue;

    public MessageListener(Future<UUID> uuidFuture, HashMap sessionMap, BlockingQueue queue) {
        this.uuidFuture = uuidFuture;
        this.queue = queue;
        this.sessionMap = sessionMap;
    }

    @Override
    public void run() {
        try {
            listen();
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                session.getSocket().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void listen() throws IOException, ExecutionException, InterruptedException {

        UUID uuid = uuidFuture.get();
        session = sessionMap.get(uuid);
        BufferedReader in = session.getIn();
        while (true) {
            String jsonMessage = in.readLine();
            System.out.println("получил текст");
            queue.put(jsonMessage);
            System.out.println("Pomestil v ochered");
        }


    }
}
