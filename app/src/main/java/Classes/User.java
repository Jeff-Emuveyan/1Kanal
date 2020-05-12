package Classes;

import com.orm.SugarRecord;

public class User extends SugarRecord<User> {

    private String userName;

    public User(){
    }

    public User(String userName){
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
