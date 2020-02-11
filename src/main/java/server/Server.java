package server;

import util.Session;
import util.User;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.*;


public class Server {
    public static final int PORT = 5555;
    ServerSocket serverSocket;
    public static HashMap<Integer, User> userMap = new HashMap<>();
    //пресистентная мапа, загружаем её при старте сервера из бд
    private static HashMap<String, HashSet<Integer>> roomMap = new HashMap<>();
    // Мапа для комнат Enum содержит список названий комнат, в листе лежат ID user которые внутри комнаты(пресистентна)
    public static HashMap<UUID, Session> sessionMap = new HashMap<>();
    // Мапа активных пользователей
    public static BlockingQueue<String> queue = new ArrayBlockingQueue<>(5);
    // Очередь куда мы помещаем сообщения в Json чтобы их потом разбирал воркер

    public static void main(String[] args) {
        Server server = new Server(PORT);


    }


    public Server(int PORT) {

        try {
            serverSocket = new ServerSocket(PORT);
            DbWorker dbWorker = new DbWorker();
            ExecutorService executor = Executors.newCachedThreadPool();
            roomMap = dbWorker.loadRoomMap();
            userMap = dbWorker.loadUsers();
            executor.submit(new MessageWorker(queue, userMap, sessionMap, roomMap));
            while (true) {
                System.out.println("rdy to connect");
                Socket clientSocket = serverSocket.accept();
                Future<UUID> uuid = executor.submit(new Authenticate(clientSocket, userMap, sessionMap, roomMap));
                executor.submit(new MessageListener(uuid, sessionMap, queue));
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                serverSocket.close();
            } catch (IOException ex) {
                e.printStackTrace();
            }
        }

    }

}