package client;

import java.util.UUID;

public class MyInfo {
    private static UUID uuid;
    private int myId;
    private static MyInfo myInfo;

    private void MyInfo() {
    }

    public static MyInfo getInstance() {
        if (myInfo == null) {
            myInfo = new MyInfo();
            uuid = UUID.randomUUID();
            return myInfo;
        } else return myInfo;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public int getMyId() {
        return myId;
    }

    public void setMyId(int myId) {
        this.myId = myId;
    }
}
