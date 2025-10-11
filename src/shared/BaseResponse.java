package shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public class BaseResponse {
    public boolean ok;
    public String message;
    public Map<String, Object> data = new HashMap<>();

    public BaseResponse() {}

    @JsonCreator
    public BaseResponse(
            @JsonProperty("ok") boolean ok,
            @JsonProperty("message") String message) {
        this.ok = ok;
        this.message = message;
    }

    public BaseResponse add(String key, Object value) {
        data.put(key, value);
        return this;
    }
}
