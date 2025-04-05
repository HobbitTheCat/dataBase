package Server;

import NewQuery.Result;
import NewQuery.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionExecutor implements Runnable {
    private final TransactionBuffer buffer;
    private volatile boolean running = true;

    public TransactionExecutor(TransactionBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void run() {
        while (running) {
            TransactionTask task = buffer.poll();
            if (task != null) {
                Transaction transaction = task.getTransaction();
                System.out.println("Выполняется транзакция: " + transaction.getTransactionId());

                List<Result> results = this.executeTransaction(transaction);
                buffer.completeTransaction(transaction.getTransactionId(), results);
            } else {
                // If the buffer is empty, wait a bit
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private List<Result> executeTransaction(Transaction transaction) { //test function
        List<Result> results = new ArrayList<>();
        for (int i = 0; i < transaction.getQueries().length; i++) {
            results.add(new Result("OK"));
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return results;
    }

    public void stop() {
        this.running = false;
    }
}
