package ServerWithAuthentication;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Name of class: ClientManager
 * <p>
 * Description: Manages client authentication and client IDs
 * <p>
 * Date 04/06
 */
public class ClientManager {
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final AtomicInteger clientIdGenerator = new AtomicInteger(1);

    /**
     * Registers a new client in the system
     * @param login client login
     * @param password client password
     * @return created client with assigned ID
     */
    public Client registerClient(String login, String password) {
        Client client = new Client(login, password);
        client.setClientId(clientIdGenerator.getAndIncrement());
        clients.put(login, client);
        System.out.println("Registered new client: " + client);
        return client;
    }

    /**
     * Authenticates the client by login and password
     * @param login client login
     * @param password client password
     * @return client with its ID or null if authentication fails
     */
    public Client authenticateClient(String login, String password) {
        Client client = clients.get(login);
        if (client != null && client.getPassword().equals(password)) {
            System.out.println("Client authenticated: " + client);
            return client;
        }
        System.out.println("Authentication failed for login: " + login);
        return null;
    }

    /**
     * Checks the existence of a client by its ID
     * @param clientId client ID
     * @return true if the client exists
     */
    public boolean isValidClientId(int clientId) {
        return clients.values().stream().anyMatch(client -> client.getClientId() == clientId);
    }

    /**
     * Returns the client by his login
     * @param login client login
     * @return client object or null if not found
     */
    public Client getClientByLogin(String login) {
        return clients.get(login);
    }
}
