package Server;

import NewQuery.Result;
import NewQuery.Transaction;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.*;
import java.net.Socket;
import java.util.*;


public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final TransactionBuffer buffer;

    public ClientHandler(Socket clientSocket, TransactionBuffer buffer) {
        this.clientSocket = clientSocket;
        this.buffer = buffer;
    }

    @Override
    public void run() {
        try (
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())
        ) {
            // Reading a transaction from a client
            Transaction transaction = (Transaction) in.readObject();
            System.out.println("Transaction received: " + transaction.getTransactionId());

            // Creating a task and adding it to the buffer
            TransactionTask task = new TransactionTask(transaction);
            buffer.add(task);

            // Expectation of results
            List<Result> results = task.getResultFuture().get();

            // Sending the result to the client
            out.writeObject(results);
            System.out.println("Result sent for transaction: " + transaction.getTransactionId());

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
}
