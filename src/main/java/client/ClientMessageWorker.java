package client;

import util.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;

import static util.MessageType.*;

public class ClientMessageWorker implements Runnable {
    private static Socket socket;
    private Helper helper = new Helper();
    private MyInfo myInfo;
    private BlockingQueue<Message> queue;
    private HashMap<Integer, User> userMap;
    private BufferedWriter out;

    public ClientMessageWorker(Socket socket, MyInfo myInfo, BlockingQueue queue, HashMap userMap) throws IOException {
        this.socket = socket;
        this.myInfo = myInfo;
        this.queue = queue;
        this.userMap = userMap;
        out= new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    @Override
    public void run() {
        while (true) {
            try {
                //берем сообщение из очереди, и чекаем его
                Message message = queue.take();
                switch (message.getMessageType()) {
                    case TEXTFROMUSER:
                        TextMessage textMessage = (TextMessage) message;
                        String text = textMessage.getText();
                        //первым делом мы проверяем не пытается ли Юзер ввести команду
                        //для этого разбиваем текст на части и проверяем первый символ
                        char c = text.charAt(0);
                        String[] textParts = text.split(" ");
                        if (c == '/') {
                            //если первый символ / это команда, уходим в метод с командами
                            commandsAndEtc(textParts);
                        } else {
                            //теперь мы проверяем указал ли пользователь имя комнаты
                            if (c == '@') {
                                messageForRoom(textParts);
                            } else {
                                System.out.println("Нет имени комнаты, пожалуйста используйте @имяКомнаты перед сообщением");
                            }
                        }
                        break;
                    case TEXTFROMSERVER:
                    case ERROR:
                    case ROOMNAMESET:
                        //в этих случаях нам нужно просто вывести сообщение на экран
                        textMessage = (TextMessage) message;
                        System.out.println(textMessage.getText());
                        break;
                    case NEWUSER:
                        // этот случай означает, что в одну из наших комнат вступил новый юзер, проверяем
                        // есть ли у нас инфа о нем, и если нет то добавляем
                        UserInfoMessage userInfoMessage = (UserInfoMessage) message;
                        if (!userMap.containsKey(userInfoMessage.getUser().getPublicUserInfo().getId())) {
                            userMap.put(userInfoMessage.getUser().getPublicUserInfo().getId(), userInfoMessage.getUser());
                        }
                        break;
                    case USERSINROOM:
                        // этот случай приходит, когда мы вступаем в новую комнату, мы получаем сэт юзеров в этой комнате
                        // проверяем знакомы ли мы с ними, если нет, то добавляем в мапу
                        UserSetMessage userSetMessage = (UserSetMessage) message;
                        HashSet<User> users = userSetMessage.getSet();
                        for (User user : users) {
                            if (!userMap.containsKey(user.getPublicUserInfo().getId())) {
                                userMap.put(user.getPublicUserInfo().getId(), user);
                            }
                        }
                        break;
                    case TEXTFROMROOM:
                        // нам пришло сообщение из комнаты, разоваричваем его и печатаем в консоль
                        textMessage = (TextMessage)message;
                        System.out.println(textMessage.getRoom() +": " + userMap.get(textMessage.getId()).getPublicUserInfo().getRealName()+ ":" + textMessage.getText());
                }

//                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }

        }

    }

    private void messageForRoom(String[] textParts) throws IOException {
        TextMessage textMessage = new TextMessage();
        //когда мы узнали что пользователь пишет в комнату собираем полноценный текст
        // нулевой член массива -это имя комнаты, из остальных собираем строку и отправляем на сервер
        textMessage.setRoom(textParts[0]);
        String messageText = new String();
        for (int i = 1; i < textParts.length; i++) {
            messageText += textParts[i] + " ";
        }
        textMessage.setText(messageText);
        textMessage.setMessageType(TEXTFORROOM);
        textMessage.setId(myInfo.getMyId());
        helper.send(out, textMessage);
    }

    private CommandMessage commandMaker(String[] textParts) throws IOException {
        CommandMessage commandMessage = new CommandMessage();
        if (textParts.length > 1) {
            // если в массиве больше одного значения, значит эта команда с аргуементом
            commandMessage.setCommand(textParts[0]);
            commandMessage.setCommandArgument(textParts[1]);
            commandMessage.setId(myInfo.getMyId());
            commandMessage.setMessageType(COMMAND);
        } else {
            //если нет, аргумент не нужен
            commandMessage.setCommand(textParts[0]);
            commandMessage.setId(myInfo.getMyId());
            commandMessage.setMessageType(COMMAND);
        }
        return commandMessage;

    }

    private void commandsAndEtc(String[] textParts) throws IOException {
        //проверяем по первому слову в text parts имя команды, чтобы понять, это команда выполняется на сервере или на клиенте
        switch (textParts[0]) {
            case ("/help"):
                System.out.println("/help, /getUser логин, /");
                break;
            default:
                //если команда выполняется на сервере уходим в метод с командами
                commandsForServer(textParts);
                break;

        }

    }

    private void commandsForServer(String[] textParts) throws IOException {
        // используем специальный метод, чтобы из стрингового массива собрать CommandMessage
        CommandMessage commandMessage = commandMaker(textParts);
        switch (commandMessage.getCommand()) {
            // эти команды требуеют проверки на наличие имени комнаты, поэтому, если имени комнаты нет
            // отправляем в консоль сообщение об этом, если есть отправляем команду на сервер
            case ("/join"):
            case ("/create"):
                if (commandMessage.getCommandArgument() == null) {
                    System.out.println("нет имени комнаты");
                } else {
                    helper.send(out, commandMessage);
                }
                break;
            // в этом случае мы просто отправляем команду на сервер
            case ("/rooms"):
                helper.send(out, commandMessage);
                break;
            default:
                // если мы не нашли такой команды выводим в консоль сообщение об этом
                System.out.println("ошибка ввода, не знаю такую команду");
                break;
        }
    }


}





