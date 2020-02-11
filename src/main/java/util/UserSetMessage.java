package util;

import java.util.HashSet;
import java.util.Set;

public class UserSetMessage extends Message{
    private HashSet<User> set = new HashSet() {
    };

    public HashSet<User> getSet() {
        return set;
    }

    public void setSet(HashSet<User> set) {
        this.set = set;
    }
}
