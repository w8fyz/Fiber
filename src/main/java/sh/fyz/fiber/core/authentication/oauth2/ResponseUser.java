package sh.fyz.fiber.core.authentication.oauth2;

import java.util.Map;

public class ResponseUser<T> {

    private T user;
    private Map<String, String> data;
    private String state;

    public ResponseUser(T user, String state, Map<String, String> data) {
        this.user = user;
        this.data = data;
        this.state = state;
    }

    public T getUser() {
        return user;
    }

    public Map<String, String> getData() {
        return data;
    }

    public String getState() {
        return state;
    }
}
