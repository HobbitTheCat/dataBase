package Server;

import NewQuery.Result;
import NewQuery.Transaction;
import TableManager.QueryToAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Name of class: TransactionExecutor
 * <p>
 * Description: Executes transactions.
 * <p>
 * Version: 1.0
 * <p>
 * Date 04/06
 * <p>
 * Copyright: Semenov Egor
 */

public class TransactionExecutor implements Runnable {
    private final QueryToAction queryExecutor;
    private final TransactionBuffer buffer;
    private volatile boolean running = true;

    public TransactionExecutor(TransactionBuffer buffer, QueryToAction queryExecutor) {
        this.buffer = buffer;
        this.queryExecutor = queryExecutor;
    }

    @Override
    public void run() {
        while (running) {
            TransactionTask task = buffer.poll();
            if (task != null) {
                Transaction transaction = task.getTransaction();
                System.out.println("Transaction in progress: " + transaction.getTransactionId());

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
            results.add(this.queryExecutor.queryRun(transaction.getQueries()[i]));
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
