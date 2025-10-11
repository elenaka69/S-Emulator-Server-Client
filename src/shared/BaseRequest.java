package shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public class BaseRequest {
    public String action;
    public Map<String, Object> data = new HashMap<>();

    public BaseRequest() {} // חובה לג׳קסון

    @JsonCreator
    public BaseRequest(@JsonProperty("action") String action) {
        this.action = action;
    }

    public BaseRequest add(String key, Object value) {
        data.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return "BaseRequest{action='" + action + "', data=" + data + "}";
    }
}
