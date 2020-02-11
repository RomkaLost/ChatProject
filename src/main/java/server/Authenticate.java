package server;

import com.google.gson.Gson;
import util.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import static util.MessageType.*;

public class Authenticate implements Callable<UUID> {
    private Socket socket;
    private UUID uuid;
    private BufferedReader in;
    private AuthMessage authMessage = new AuthMessage();
    private String jsonMessage;
    private DbWorker db = new DbWorker();
    private Gson gson = new Gson();
    private Helper helper = new Helper();
    private HashMap<Integer, User> userMap;
    private HashMap<UUID, Session> sessionMap;
    private HashMap<String, HashSet<Integer>> roomMap;
    private Session session;

    public Authenticate(Socket socket, HashMap userMap, HashMap sessionMap, HashMap roomMap) {
        this.socket = socket;
        this.userMap = userMap;
        this.sessionMap = sessionMap;
        this.roomMap = roomMap;
    }

    @Override
    public UUID call() throws IOException {
        // Объявил необходимые переменные.
        session = new Session(socket);
        in= session.getIn();
        // После хэндшейка клиент отправляет сообщение с UUID
        jsonMessage = session.getIn().readLine();
        // Сообщение в Json мы распаковываем в AuthMessage т.к. только AuthMessage содержит UUID
        authMessage = gson.fromJson(jsonMessage, AuthMessage.class);
        // Получаем UUID из пришедшего сообщения чтобы решить, что делать дальше
        uuid = authMessage.getUuid();
        if (sessionMap.containsKey(uuid)) {
            // Если UUID нам уже знаком, то мы поднимаем из базы данных пользователей инфу о User и замеяем Socket на новый
            // сообщаем клиенту, что все готово для работы
            sessionMap.get(uuid).setSocket(socket);
            authMessage.setMessageType(WEHAVEUUID);
            helper.send(session.getOut(), authMessage);
        } else {
            //сообщаем клиенту, что мы с ним не знакомы, высылаем ему новый UUID
            authMessage.setMessageType(NEWUUID);
            uuid = UUID.randomUUID();
            authMessage.setUuid(uuid);
            helper.send(session.getOut(), authMessage);
            //получаем от клиента сообщение, c регистрационными данными чтобы понять, хочет ли он регистрироваться
            // или авторизовываться
            boolean result = false;
            while (!result) {
                jsonMessage = in.readLine();
                UserInfoMessage userInfoMessage = gson.fromJson(jsonMessage, UserInfoMessage.class);
                if (userInfoMessage.getMessageType() == REGISTRATION) {
                    result = registration(userInfoMessage, session);
                } else {
                    result = authorisation(userInfoMessage);
                }
            }
        }
        return uuid;
    }

    public boolean registration(UserInfoMessage userInfoMessage, Session session) throws IOException {
        boolean result = false;
        User user = userInfoMessage.getUser();
        if (db.checkLogin(user.getPrivateUserInfo().getLogin()) == true) {
            //если через дб воркер, мы выяснили, что такого логина нет в базе,регаем нового юзера
            userInfoMessage.setMessageType(NEWUSER);
            helper.send(session.getOut(), userInfoMessage);
            //получаю от клиента сообщение с регистрационными данными
            jsonMessage = session.getIn().readLine();
            userInfoMessage = gson.fromJson(jsonMessage, UserInfoMessage.class);
            user.setPublicUserInfo(userInfoMessage.getUser().getPublicUserInfo());
            //добавляем нового пользователя в БД
            db.makeNewUser(user);
            //добавляем нового пользователя в мапы
            user.getPrivateUserInfo().setUuid(uuid);
            session.setUser(user);
            session.setSocket(socket);
            sessionMap.put(uuid, session);
            userMap.put(user.getPublicUserInfo().getId(), user);
            //отправляем подтверждение окончания регистрации
            userInfoMessage.setMessageType(NEWUSER);
            helper.send(session.getOut(), userInfoMessage);
            result = true;
        } else {
            authMessage.setMessageType(LOGINERROR);
            helper.send(session.getOut(), authMessage);
        }
        return result;
    }

    public boolean authorisation(UserInfoMessage userInfoMessage) throws IOException {
        boolean result = false;
        boolean check = db.checkLogPass(userInfoMessage.getUser());
        if (check) {
            //если такие данные есть, дергаем из мапы юзеров юзера, и привязываем ему новый UUID
            User user = userMap.get(userInfoMessage.getUser().getPublicUserInfo().getId());
            user.getPrivateUserInfo().setUuid(uuid);
            // Собираем сессию и помещаем все это в мапу с сессиями
            session.setUser(user);
            sessionMap.put(uuid, session);
            //авторизация прошла успешно поэтому меняем значение возвращаемой переменной на успех
            //и отправляем подтверждающее сообщение на клиент
            result = true;
            userInfoMessage.setId(user.getPublicUserInfo().getId());
            userInfoMessage.setMessageType(AUTH);
            helper.send(session.getOut(), userInfoMessage);
            //теперь мы собираем сэт юзеров для нашего пользователя и отправляем
            HashSet users = createUserSet(userMap, roomMap, userInfoMessage.getId());
            sendUsers(session, users);
        } else {
            authMessage.setMessageType(LOGINERROR);
            helper.send(session.getOut(), authMessage);
        }

        return result;
    }

    private HashSet<User> createUserSet(HashMap<Integer, User> userMap, HashMap<String, HashSet<Integer>> roomMap, int userId) {
        HashSet<User> users = new HashSet<>();
        //Мы бежим по значениям в мапе с комнатами и если авторизированный юзер состоит в этой комнате, ложим в сэт
        //остальных юзеров из этой комнаты
        for (Set<Integer> set : roomMap.values()) {
            if (set.contains(userId)) {
                for (Integer id : set) {
                    User user = userMap.get(id);
                    User publicUser = new User();
                    publicUser.setPublicUserInfo(user.getPublicUserInfo());
                    users.add(publicUser);
                }
            }
        }
        return users;
    }

    private void sendUsers(Session session, HashSet userSet) throws IOException {
        UserSetMessage userSetMessage = new UserSetMessage();
        userSetMessage.setSet(userSet);
        helper.send(session.getOut(), userSetMessage);

    }
}
