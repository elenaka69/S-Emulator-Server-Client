package shared;

public class LoginResponse {
    public boolean ok;
    public String message;
    public LoginResponse() {}
    public LoginResponse(boolean ok, String msg) { this.ok = ok; this.message = msg; }
}
