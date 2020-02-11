package util;

public class TextMessage extends Message {
    String Room;
    String text;

    public String getRoom() {
        return Room;
    }

    public void setRoom(String room) {
        this.Room = room;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
