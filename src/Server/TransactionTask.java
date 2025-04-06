package Server;

import NewQuery.Result;
import NewQuery.Transaction;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Name of class: TransactionTask
 * <p>
 * Description: Wraps Transactions with nice future sauce into the Transaction buffer.
 * <p>
 * Version: 1.0
 * <p>
 * Date 04/06
 * <p>
 * Copyright: Lemain Mathieu
 */

public class TransactionTask {
    private final Transaction transaction;
    private final CompletableFuture<List<Result>> resultFuture;

    public TransactionTask(Transaction transaction) {
        this.transaction = transaction;
        this.resultFuture = new CompletableFuture<>();
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public CompletableFuture<List<Result>> getResultFuture() {
        return resultFuture;
    }
}
