import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class QueryBuffer {
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private ArrayList<Query> queries = new ArrayList<Query>();
    public void appendQueryList(ArrayList<Query> queries){
        lock.writeLock().lock();
        try{
            this.queries.addAll(queries);
        }
        finally{
            lock.writeLock().unlock();
        }
    }
    public ArrayList<Query> getQueryList(){
        lock.readLock().lock();
        try{
            return queries;
        }
        finally{
            lock.readLock().unlock();
        }
    }
}