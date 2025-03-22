package PageManager;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Resource {
    private final int id;
    private Thread owner = null;
    private final Lock lock = new ReentrantLock();

    public Resource(int id) {
        this.id = id;
    }

    public void lock() {this.lock.lock();}
    public void unlock() {this.lock.unlock();}

    public int getId() {
        return this.id;
    }

    public Thread getOwner() {return this.owner;}
    public void setOwner(Thread owner) {this.owner = owner;}

    @Override
    public String toString() {
        return "Resource-" + this.id;
    }
}
