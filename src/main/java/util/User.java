package util;

public class User {
    PublicUserInfo publicUserInfo = new PublicUserInfo();
    PrivateUserInfo privateUserInfo = new PrivateUserInfo();

    public PublicUserInfo getPublicUserInfo() {
        return publicUserInfo;
    }

    public void setPublicUserInfo(PublicUserInfo publicUserInfo) {
        this.publicUserInfo = publicUserInfo;
    }

    public PrivateUserInfo getPrivateUserInfo() {
        return privateUserInfo;
    }

    public void setPrivateUserInfo(PrivateUserInfo privateUserInfo) {
        this.privateUserInfo = privateUserInfo;
    }

    public void setPublic(String realname, String gender, int id){
        publicUserInfo.setGender(gender);
        publicUserInfo.setRealName(realname);
        publicUserInfo.setId(id);
    }
    public void setPrivate(String login, String password){
        privateUserInfo.setLogin(login);
        privateUserInfo.setPassword(password);

    }
}
