package sh.fyz.fiber.util;

import java.util.Map;

public class ResponseContext<T> {

    private T entity;
    private Map<String, String> data;
    private String state;

    public ResponseContext(T entity, String state, Map<String, String> data) {
        this.entity = entity;
        this.data = data;
        this.state = state;
    }

    public T getResult() {
        return entity;
    }

    public Map<String, String> getData() {
        return data;
    }

    public String getState() {
        return state;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public void setState(String state) {
        this.state = state;
    }
}
