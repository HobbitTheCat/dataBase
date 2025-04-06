package Server;

import NewQuery.Result;
import NewQuery.Transaction;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionBuffer {
    private final ConcurrentLinkedQueue<TransactionTask> buffer = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Integer, CompletableFuture<List<Result>>> resultMap = new ConcurrentHashMap<>();
    private final AtomicInteger transactionIdGenerator = new AtomicInteger(1);

    /**
     * Generate unique transaction ID
     * @return new unique ID
     */
    public int generateTransactionId() {
        return transactionIdGenerator.getAndIncrement();
    }

    public void add(TransactionTask task) {
        Transaction transaction = task.getTransaction();
        if(transaction.getTransactionId() == -1)
            transaction.changeTransactionId(generateTransactionId());

        buffer.add(task);
        resultMap.put(transaction.getTransactionId(), task.getResultFuture());
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
