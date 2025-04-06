package ServerWithAuthentication;

import NewQuery.Result;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.*;
import java.net.Socket;
import java.util.*;


public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final TransactionBuffer buffer;
    private final ClientManager clientManager;

    public ClientHandler(Socket clientSocket, TransactionBuffer buffer, ClientManager clientManager) {
        this.clientSocket = clientSocket;
        this.buffer = buffer;
        this.clientManager = clientManager;
    }

    @Override
    public void run() {
        try (
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())
        ) {
            String command = (String) in.readObject();

            if ("AUTHENTICATE".equals(command)) {
                handleAuthentication(in, out);
            } else if ("TRANSACTION".equals(command)) {
                handleTransaction(in, out);
            } else {
                System.err.println("Unknown command: " + command);
            }

        } catch (Exception e) {
            System.err.println("Error while processing client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Processes the authentication request
     */
    private void handleAuthentication(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        // Read client data
        Client requestClient = (Client) in.readObject();

        // Authenticating the client
        Client authenticatedClient = clientManager.authenticateClient(
                requestClient.getLogin(), requestClient.getPassword());

        if (authenticatedClient != null) {
            // Successful authentication
            out.writeObject(Boolean.TRUE);
            out.writeObject(authenticatedClient);
            System.out.println("Client authenticated: " + authenticatedClient);
        } else {
            // Authentication error
            out.writeObject(Boolean.FALSE);
            System.out.println("Authentication failed for: " + requestClient.getLogin());
        }
        out.flush();
    }

    /**
     * Processes a request to execute a transaction
     */
    private void handleTransaction(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        // Reading a transaction from a client
        Transaction transaction = (Transaction) in.readObject();

        // Checking the validity of the client ID if it is set
        int clientId = transaction.getClientId();
        if (clientId != -1 && !clientManager.isValidClientId(clientId)) {
            System.err.println("Invalid client ID in transaction: " + clientId);
            List<Result> errorResults = new ArrayList<>();
            // Here you can add a Result object with an authentication error
            out.writeObject(errorResults);
            return;
        }

        // Creating a task and adding it to the buffer
        TransactionTask task = new TransactionTask(transaction);
        buffer.add(task);

        System.out.println("Transaction received and assigned ID: " + transaction.getTransactionId() +
                " from client: " + (clientId != -1 ? clientId : "unauthenticated"));

        List<Result> results = task.getResultFuture().get();

        out.writeObject(results);
        System.out.println("Result sent for transaction: " + transaction.getTransactionId());
    }
}
