package NewQuery;

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
 * Description: Used as a client for executing a transaction.
 * <p>
 * Version: 3.0
 * <p>
 * Date 04/04
 * <p>
 * Copyright: Semenov Egor, Lemain Mathieu
 */

public class Session implements Closeable {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public Session(String host, int port) {
        this.host = host;
        this.port = port;
        this.connect();
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
     * Sending a transaction and receiving the result
     */
    public List<Result> execute(Transaction transaction) throws IOException, ClassNotFoundException {
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
}
