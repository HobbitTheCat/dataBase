package Server;

import NewQuery.Result;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TransactionBuffer {
    private final ConcurrentLinkedQueue<TransactionTask> buffer = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Integer, CompletableFuture<List<Result>>> resultMap = new ConcurrentHashMap<>();

    public void add(TransactionTask task) {
        buffer.add(task);
        resultMap.put(task.getTransaction().getTransactionId(), task.getResultFuture());
    }

    public TransactionTask poll() {
        return buffer.poll();
    }

    public void completeTransaction(int transactionId, List<Result> results) {
        CompletableFuture<List<Result>> future = resultMap.remove(transactionId);
        if (future != null) {
            future.complete(results);
        }
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }
}
