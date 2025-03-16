import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class VirtualMemoryManager {
    private final int PageSize = 4096;
    private final int maxPage;
    private final PageSaver saver;
    private final PageLoader loader;

    private final ConcurrentHashMap<Integer, ByteBuffer> pageCache;
    private final ConcurrentHashMap<Integer, ReentrantReadWriteLock> pageLocks;
    private final ConcurrentSkipListSet<Integer> dirtyPages;
    private final ConcurrentHashMap<Integer, Integer> accessCounts = new ConcurrentHashMap<>(); //LFU strategy

    public VirtualMemoryManager(int maxPage, PageSaver saver, PageLoader loader) {
        this.maxPage = maxPage;
        this.pageCache = new ConcurrentHashMap<>(maxPage);
        this.pageLocks = new ConcurrentHashMap<>();
        this.dirtyPages = new  ConcurrentSkipListSet<>();
        this.saver = saver;
        this.loader = loader;
    }

    public ByteBuffer loadPage(int pageIndex) {
        this.pageLocks.putIfAbsent(pageIndex, new ReentrantReadWriteLock());
        ReentrantReadWriteLock lock = this.pageLocks.get(pageIndex);

        lock.readLock().lock();
        try{
            ByteBuffer page = this.pageCache.get(pageIndex);
            if(page != null){
                this.accessCounts.merge(pageIndex, 1, Integer::sum);
                return page.duplicate();
            }
        } finally{
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try{
            ByteBuffer page = this.pageCache.get(pageIndex);
            if(page == null){
                page = this.loader.load(pageIndex);
                if(this.pageCache.size() >= this.maxPage){
                    this.evictPage();
                }
                this.pageCache.put(pageIndex, page);
                this.accessCounts.put(pageIndex, 1);
            }
            return page.duplicate();
        }finally{
            lock.writeLock().unlock();
        }
    }

    public void markDirty(int pageIndex) {
        this.accessCounts.merge(pageIndex, 1, Integer::sum);
        this.dirtyPages.add(pageIndex);
    }

    public void flushPage(int pageIndex) {
        this.pageLocks.putIfAbsent(pageIndex, new ReentrantReadWriteLock());
        ReentrantReadWriteLock lock = this.pageLocks.get(pageIndex);

        lock.writeLock().lock();
        try{
            ByteBuffer page = this.pageCache.get(pageIndex);
            if(page != null && this.dirtyPages.contains(pageIndex)){
                this.saver.save(pageIndex, page);
                this.dirtyPages.remove(pageIndex);
            }
        }  finally{
            lock.writeLock().unlock();
        }
    }

    public void flushAll() {
        for(int pageIndex : this.dirtyPages){
            this.flushPage(pageIndex);
        }
    }

    public void unloadPage(int pageIndex) {
        this.pageLocks.putIfAbsent(pageIndex, new ReentrantReadWriteLock());
        ReentrantReadWriteLock lock = this.pageLocks.get(pageIndex);

        lock.writeLock().lock();
        try{
            this.pageCache.remove(pageIndex);
            this.dirtyPages.remove(pageIndex);
            this.accessCounts.remove(pageIndex);
        } finally{
            lock.writeLock().unlock();
        }
    }

    private void evictPage() {
        Integer pageIndexToRemove = this.accessCounts.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (pageIndexToRemove == null) return;

        ReentrantReadWriteLock lock = pageLocks.get(pageIndexToRemove);
        if (lock == null) return;

        lock.writeLock().lock();
        try {
            if (dirtyPages.contains(pageIndexToRemove)) {
                flushPage(pageIndexToRemove);
            }
            pageCache.remove(pageIndexToRemove);
            dirtyPages.remove(pageIndexToRemove);
            accessCounts.remove(pageIndexToRemove);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void ageAccessCounts() {
        accessCounts.replaceAll((pageIndex, count) -> count / 2);
    }

    interface PageLoader { ByteBuffer load(int pageIndex);}
    interface PageSaver {void save(int pageIndex, ByteBuffer data);}
}
