import Pages.Page;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PageManager {
    private final int maxCachedPages;
    private final Map<Integer, Page> pageCache;
    private final Set<Integer> dirtyPages;
    private final Map<Integer, Integer> accessCount = new HashMap<>();

    private final Map<Thread, Set<Page>> threadToResourceHeld;
    private final Map<Thread, Set<Page>> threadToResourceWaiting;

    private final Lock graphLock = new ReentrantLock();
    private final Condition resourceReleased = this.graphLock.newCondition();

    private final PageLoader loader;
    private final PageSaver saver;

    public PageManager(int maxCachedPages, PageLoader loader, PageSaver saver) {
        this.maxCachedPages = maxCachedPages;
        this.pageCache = new ConcurrentHashMap<>(maxCachedPages);
        this.dirtyPages = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.threadToResourceHeld = new HashMap<>();
        this.threadToResourceWaiting = new HashMap<>();
        this.loader = loader;
        this.saver = saver;
    }




    interface PageLoader { ByteBuffer load(int pageIndex);}
    interface PageSaver {void save(int pageIndex, ByteBuffer data);}
}
