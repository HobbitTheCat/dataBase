package PreviousMemoryManagers;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Resource {
    private final int id;
    private volatile Thread owner = null;
    private final Lock lock = new ReentrantLock();
    private ByteBuffer data;

    public Resource(int id, ByteBuffer data) {
        this.id = id;
        this.data = data;
    }

    public void lock() {this.lock.lock();}
    public void unlock() {this.lock.unlock();}

    public int getId() {
        return this.id;
    }
    public ByteBuffer getData() {return this.data;}

    public Thread getOwner() {return this.owner;}
    public void setOwner(Thread owner) {this.owner = owner;}
    public void setData(ByteBuffer data) {this.data = data;}

    @Override
    public String toString() {
        return "Resource-" + this.id;
    }
}
