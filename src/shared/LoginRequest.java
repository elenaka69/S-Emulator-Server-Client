package shared;

public class LoginRequest {
    public String username;

    // Default constructor (needed for Jackson)
    public LoginRequest() {}

    // Constructor with username
    public LoginRequest(String username) {
        this.username = username;
    }
}
