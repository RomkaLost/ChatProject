package client;

import com.google.gson.Gson;
import util.*;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;

import static util.MessageType.*;

public class ClientAuthenticate {
    private BufferedReader reader;
    private BufferedWriter out;
    private BufferedReader in;
    private String jsonMessage;
    private Gson gson = new Gson();
    private Helper helper = new Helper();
    private MyInfo myInfo = new MyInfo();
    private Socket socket;
    private User user = new User();
    private HashMap<Integer, User> userMap = new HashMap<>();

    public void auth(Socket socket, MyInfo myInfo, HashMap<Integer, User> users) throws IOException {
        this.socket = socket;
        this.myInfo = myInfo;
        this.userMap = users;
        Message message = new Message();
        AuthMessage authMessage = new AuthMessage();
        // объявили стримы
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(System.in));
        //добавляю UUID к сообщению
        authMessage.setUuid(myInfo.getUuid());
        authMessage.setMessageType(AUTH);
        //отправляем его на сервер
        helper.send(out, authMessage);
        //жду сообщение от сервера, чтобы проверить свой UUID
        jsonMessage = in.readLine();
        authMessage = gson.fromJson(jsonMessage, AuthMessage.class);
        if (message.getMessageType() == WEHAVEUUID) {
            System.out.println("Переподключение успешно");
            //если UUID уже есть, просто переподключаюсь (все делает сервер)
        } else {
            //если сервер не нашел моего UUID он сгенерил новый и отправил его мне
            myInfo.setUuid(authMessage.getUuid());
            // делаю запрос в командной строке, зарегистрирован ли пользователь

            boolean success = false;
            while (!success) {
                String answer = helper.readUserInput("Проходил ли ты регистрацию, путник? Y/N");
                if (answer.equalsIgnoreCase("n")) {
                    //если пользователь не зарегистрирован, мы его регистрируем
                    registration();
                    success = true;
                } else if (answer.equalsIgnoreCase("y")) {
                    //если зареган то авторизуем
                    authorisation();
                    fillUserMap(userMap);
                    success = true;
                } else {
                    System.out.println("Ошибка ввода, попробуй еще раз");
                }

            }
        }
    }

    public void registration() throws IOException {
        boolean success = false;
        Message message = new Message();
        //запускаем цикл, который закончится, когда сервер подтвердит регистрацию
        while (!success) {
            //заполняем логин и пароль и отправляем их на сервер
            UserInfoMessage userInfoMessage= new UserInfoMessage();
            logAndPass(userInfoMessage);
            userInfoMessage.setMessageType(REGISTRATION);
            helper.send(out, userInfoMessage);
            //сервер проверяет не занят ли логин, и отправляет сообщение с типом NEWUSER если все ок
            jsonMessage = in.readLine();
            userInfoMessage = gson.fromJson(jsonMessage, UserInfoMessage.class);
            if (userInfoMessage.getMessageType() == NEWUSER) {
                //прерываем цикл
                success = true;
                System.out.println("Отлично, сообщите немного о себе");
                //дозаполняем информацию о себе
                user.getPublicUserInfo().setRealName(helper.readUserInput("Укажите ваше имя"));
                user.getPublicUserInfo().setGender(helper.readUserInput("Укажите ваш пол"));
                userInfoMessage.setUser(user);
                helper.send(out, userInfoMessage);
                //ждем от сервера сообщение с нашим ID
                jsonMessage = in.readLine();
                userInfoMessage = gson.fromJson(jsonMessage, UserInfoMessage.class);
                myInfo.setMyId(userInfoMessage.getUser().getPublicUserInfo().getId());
                System.out.println("Поздравляем с успешной регистрацией!");
            } else System.out.println("Такой логин уже занят, попробуем еще раз");
        }
    }

    public void authorisation() throws IOException {
        boolean success = false;
        // запускаем цикл до окончания регистрации
        while (!success) {
            //заполняем логин и пароль и отправляем их на сервер для проверки
            UserInfoMessage userInfoMessage = new UserInfoMessage();
            logAndPass(userInfoMessage);
            userInfoMessage.setMessageType(AUTHORISATION);
            helper.send(out, userInfoMessage);
            //ждем ответ от сервера
            jsonMessage = in.readLine();
            userInfoMessage = gson.fromJson(jsonMessage, UserInfoMessage.class);
            if (userInfoMessage.getMessageType() == AUTH) {
                myInfo.setMyId(userInfoMessage.getId());
                success = true;
                System.out.println("Поздравляем с успешной авторизацией!");
            } else System.out.println("неверная пара логин-пароль, попробуем еще раз");

        }
    }

    public void logAndPass(UserInfoMessage userInfoMessage) throws IOException {
        //добавляет логин и пароль
        user.getPrivateUserInfo().setLogin(helper.readUserInput("Укажите логин"));
        user.getPrivateUserInfo().setPassword(helper.readUserInput("Укажите пароль"));
        userInfoMessage.setUser(user);

    }

    public void fillUserMap(HashMap<Integer, User> userMap) throws IOException {
        jsonMessage = in.readLine();
        UserSetMessage messageWithSet = gson.fromJson(jsonMessage, UserSetMessage.class);
        HashSet<User> users = messageWithSet.getSet();
        for (User user : users) {
            userMap.put(user.getPublicUserInfo().getId(), user);
        }

    }

}


