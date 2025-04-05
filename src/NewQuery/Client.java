package NewQuery;

import NewQuery.Exceptions.ClientException;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.List;

/**
 * Client for sending transactions to the server and receiving the results
 */
public class Client implements Closeable {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Connecting to the server
     */
    public void connect(){
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
    public List<Result> sendTransaction(Transaction transaction) throws IOException, ClassNotFoundException {
        // Send transaction
        out.writeObject(transaction);
        out.flush();
        System.out.println("Транзакция отправлена: " + transaction.getTransactionId());

        // Getting the result
        List<Result> results = (List<Result>) in.readObject();
        System.out.println("Получены результаты для транзакции: " + transaction.getTransactionId());

        return results;
    }

    @Override
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }


    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;

        try (Client client = new Client(host, port)) {
            client.connect();

            Transaction transaction = new Transaction(1);
            transaction.add(Query.select(Hero.class).where("name", "==", "pedro"));

            List<Result> results = client.sendTransaction(transaction);
            System.out.println("Number of results obtained: " + results.size());

        } catch (Exception e) {
            System.err.println("Runtime error" + e.getMessage());
        }
    }
}