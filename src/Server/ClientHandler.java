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
            // Чтение транзакции от клиента
            Transaction transaction = (Transaction) in.readObject();
            System.out.println("Получена транзакция: " + transaction.getTransactionId());

            // Создание задачи и добавление в буфер
            TransactionTask task = new TransactionTask(transaction);
            buffer.add(task);

            // Ожидание результата
            List<Result> results = task.getResultFuture().get();

            // Отправка результата клиенту
            out.writeObject(results);
            System.out.println("Результат отправлен для транзакции: " + transaction.getTransactionId());

        } catch (Exception e) {
            System.err.println("Ошибка при обработке клиента: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Ошибка закрытия соединения: " + e.getMessage());
            }
        }
    }
}
