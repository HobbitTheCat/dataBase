package ServerWithAuthentication;

import NewQuery.Exceptions.ClientException;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.List;

/**
 * Name of class: Session
 * <p>
 * Description: Used as a client for executing a transaction with authentication.
 * <p>
 * Version: 3.1
 * <p>
 * Date 04/06
 * <p>
 * Copyright: Semenov Egor, Lemain Mathieu
 */

public class Session implements Closeable {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Client client;

    /**
     * Creates a session and connects to the server without authentication
     */
    public Session(String host, int port) {
        this.host = host;
        this.port = port;
        this.connect();
    }

    /**
     * Creates a session, connects to the server and performs authentication
     */
    public Session(String host, int port, String login, String password) {
        this.host = host;
        this.port = port;
        this.connect();
        this.authenticate(login, password);
    }

    /**
     * Connecting to the server
     */
    private void connect(){
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println("Connected to the server: " + host + ":" + port);
        }  catch (ConnectException e) {
            throw new ClientException("Connection refused server offline");
        } catch (IOException e) {
            throw new ClientException("Connection error");
        }
    }

    /**
     * Authenticates the user on the server
     * @param login user login
     * @param password user password
     * @return true if authentication is successful
     */
    public boolean authenticate(String login, String password) {
        try {
            Client tempClient = new Client(login, password);

            out.writeObject("AUTHENTICATE");
            out.writeObject(tempClient);
            out.flush();

            Boolean result = (Boolean) in.readObject();

            if (result) {
                this.client = (Client) in.readObject();
                System.out.println("Authentication successful. Client ID: " + client.getClientId());
                return true;
            } else {
                System.out.println("Authentication failed");
                return false;
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new ClientException("Authentication error: " + e.getMessage());
        }
    }

    /**
     * Sending a transaction and receiving the result
     */
    public List<Result> execute(Transaction transaction) throws IOException, ClassNotFoundException {
        // Set client ID in transaction if authenticated
        if (client != null) {
            transaction.setClientId(client.getClientId());
        }

        // Send command
        out.writeObject("TRANSACTION");

        // Send transaction
        out.writeObject(transaction);
        out.flush();
        System.out.println("Transaction sent: " + transaction.getTransactionId());

        // Getting the result
        List<Result> results = (List<Result>) in.readObject();
        System.out.println("Results obtained for the transaction: " + transaction.getTransactionId());

        return results;
    }

    @Override
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public Transaction createNewTransaction(){
        return new Transaction();
    }

    /**
     * Returns the current authenticated client
     */
    public Client getClient() {
        return client;
    }
}
