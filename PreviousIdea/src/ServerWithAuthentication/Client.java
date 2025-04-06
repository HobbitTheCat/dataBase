package ServerWithAuthentication;
import java.io.Serializable;

/**
 * Name of class: Client
 * <p>
 * Description: Represents a user of the system with authentication credentials
 * <p>
 * Date 04/06
 */
public class Client implements Serializable {
    private final String login;
    private final String password;
    private int clientId = -1;

    public Client(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    @Override
    public String toString() {
        return "Client{login='" + login + "', clientId=" + clientId + "}";
    }
}