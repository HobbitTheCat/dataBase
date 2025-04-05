package Server;

import NewQuery.Result;
import NewQuery.Transaction;

import java.util.List;
import java.util.concurrent.CompletableFuture;


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
