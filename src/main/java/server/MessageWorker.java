package server;

import com.google.gson.Gson;
import util.*;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;

import static util.MessageType.*;

public class MessageWorker implements Runnable {
    private Gson gson = new Gson();
    private HashMap<Integer, User> userMap;
    private HashMap<UUID, Session> sessionMap;
    private HashMap<String, HashSet<Integer>> roomMap;
    private BlockingQueue<String> queue;
    private Helper helper = new Helper();
    private DbWorker dbWorker = new DbWorker();

    public MessageWorker(BlockingQueue queue, HashMap userMap, HashMap sessionMap, HashMap roomMap) {
        this.userMap = userMap;
        this.sessionMap = sessionMap;
        this.queue = queue;
        this.roomMap = roomMap;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // берем сообщение из очереди, по типу понимаем с чем мы имеем дело
                String jsonMessage = queue.take();
                Message message = gson.fromJson(jsonMessage, Message.class);
                MessageType type = message.getMessageType();
                switch (type) {
                    case TEXTFORROOM:
                        roomTextMessage(jsonMessage);
                        break;
                    case COMMAND:
                        commandMessage(jsonMessage);
                        break;
                }


            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }


    }

    public void messageSend(String text) throws IOException {
        //рассылаем сообщение всем юзерам
        for (Session session : sessionMap.values()) {
            Socket socket = session.getSocket();
            if (!socket.isClosed()) {
                TextMessage textMessage = new TextMessage();
                textMessage.setText(text);
                textMessage.setMessageType(TEXTFROMSERVER);
                helper.send(session.getOut(), textMessage);
                System.out.println("Написал " + text);
            }
        }
    }


    public void roomTextMessage(String jsonMessage) throws IOException {
        TextMessage textMessage = gson.fromJson(jsonMessage, TextMessage.class);
        textMessage.setMessageType(TEXTFROMROOM);
        String roomName = textMessage.getRoom();
        if (roomMap.containsKey(roomName)) {
            HashSet<Integer> inRoom = roomMap.get(roomName);
            for (Session session : sessionMap.values()) {
                Socket socket = session.getSocket();
                if (!socket.isClosed()) {
                    if (inRoom.contains(session.getUser().getPublicUserInfo().getId())) {
                        helper.send(session.getOut(), textMessage);
                        System.out.println("Написал " + jsonMessage);
                    }
                }
            }
        } else {
            helper.error(sessionTaker(textMessage.getId()), "Такой комнаты не существует");
        }
    }

    private void commandMessage(String jsonMessage) throws IOException {

        CommandMessage commandMessage = gson.fromJson(jsonMessage, CommandMessage.class);
        String command = commandMessage.getCommand();
        String commandValue = commandMessage.getCommandArgument();
        String roomName = "@" + commandValue;
        Integer userId = commandMessage.getId();
        switch (command) {
            case ("/join"):
                if (roomMap.containsKey(roomName)) {
                    if (!roomMap.get(roomName).contains(userId)) {
                        dbWorker.joinRoom(roomName, userId);
                        roomMap.get(roomName).add(userId);
                        System.out.println("добавил пользователя " + userId);
                        messageSend("Ура! Пользователь " + userMap.get(userId).getPublicUserInfo().getRealName() + " вступил в комнату " + roomName);
                        newUserInRoom(userMap.get(userId), roomName);
                    } else helper.error(sessionTaker(userId), "Вы уже состоите в этой комнтае");
                } else helper.error(sessionTaker(userId), "Такой комнаты не существует");
                break;
            case ("/create"):
                if (roomMap.containsKey(roomName)) {
                    helper.error(sessionTaker(userId), "Такая комната уже существует");
                } else {
                    HashSet<Integer> newList = new HashSet<>();
                    newList.add(userId);
                    roomMap.put(roomName, newList);
                    dbWorker.createNewRoom(roomName, userId);
                    messageSend("Ура! Пользователь " + userMap.get(userId).getPublicUserInfo().getRealName() + " создал комнату " + roomName);
                    System.out.println("создал комнату " + commandMessage.getCommandArgument());
                }
                break;
            case ("/rooms"):
                TextMessage textMessage = new TextMessage();
                textMessage.setMessageType(ROOMNAMESET);
                textMessage.setText(roomMap.keySet().toString());
                helper.send(sessionTaker(userId).getOut(), textMessage);
                break;
            case ("/leave"):
                if (roomMap.containsKey(roomName)) {
                    if (roomMap.get(roomName).contains(userId)) {
                        dbWorker.leaveRoom(userId, roomName);
                        roomMap.get(roomName).remove(userId);
                        messageSend("Пользователь " + userMap.get(userId).getPublicUserInfo().getRealName() + " покинул комнату " + roomName);

                    } else {
                        helper.error(sessionTaker(userId), "Вы не состоите в этой комнате");
                    }
                } else {
                    helper.error(sessionTaker(userId), "Такой комнаты не существует");
                }
                break;
            default:
                Session session = sessionMap.get(userMap.get(userId).getPrivateUserInfo().getUuid());
                helper.error(session, "Ошибка ввода");
        }
    }

    private Session sessionTaker(Integer id) {
        UUID uuid = userMap.get(id).getPrivateUserInfo().getUuid();
        Session session = sessionMap.get(uuid);
        return session;
    }

    public void newUserInRoom(User user, String roomName) throws IOException {
        UserInfoMessage userInfoMessage = new UserInfoMessage();
        User userToSend = new User();
        userToSend.setPublicUserInfo(user.getPublicUserInfo());
        userInfoMessage.setUser(userToSend);
        userInfoMessage.setMessageType(NEWUSER);
        HashSet<Integer> inRoom = roomMap.get(roomName);
        for (Session session : sessionMap.values()) {

            if (!session.getSocket().isClosed()) {
                if (inRoom.contains(session.getUser().getPublicUserInfo().getId())) {
                    helper.send(session.getOut(), userInfoMessage);
                }
            }
        }
        HashSet<User> usersInRoom = new HashSet<>();
        for (Integer id : inRoom) {
            usersInRoom.add(userMap.get(id));
        }
        UserSetMessage userSetMessage = new UserSetMessage();
        userSetMessage.setSet(usersInRoom);
        userSetMessage.setMessageType(USERSINROOM);
        helper.send(sessionMap.get(user.getPrivateUserInfo().getUuid()).getOut(), userSetMessage);
    }
}

