package PreviousMemoryManagers;

import Pages.FreePage;
import Pages.HeaderPage;
import Pages.StorageOperationException;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class VirtualMemoryManagerV2_0 {
    private final int pageSize = 4096;
    private final int maxPage;
    private int pageCount;
    private final PageSaver saver;
    private final PageLoader loader;

    private final ConcurrentHashMap<Integer, ByteBuffer> pageCache;
    private final ConcurrentHashMap<Integer, ReentrantReadWriteLock> pageLocks;
    private final ConcurrentHashMap<Integer, Integer> accessCounts = new ConcurrentHashMap<>();
    private final ConcurrentSkipListSet<Integer> dirtyPages;
    private final ConcurrentHashMap<Integer, Integer> pageVersions;
    private final ReentrantLock headerLock = new ReentrantLock();

    public VirtualMemoryManagerV2_0(int maxPage, PageSaver saver, PageLoader loader) {
        this.maxPage = maxPage;
        this.pageCache = new ConcurrentHashMap<>(maxPage);
        this.pageLocks = new ConcurrentHashMap<>();
        this.pageVersions = new ConcurrentHashMap<>();
        this.dirtyPages = new  ConcurrentSkipListSet<>();

        this.saver = saver;
        this.loader = loader;
    }

    public DataWrapper loadPage(int pageIndex) {
        this.pageLocks.putIfAbsent(pageIndex, new ReentrantReadWriteLock());
        this.pageVersions.putIfAbsent(pageIndex, 0);
        ReentrantReadWriteLock lock = this.pageLocks.get(pageIndex);

        lock.readLock().lock();
        try{
            ByteBuffer page = this.pageCache.get(pageIndex);
            if (page != null) {
                this.accessCounts.merge(pageIndex, 1, Integer::sum);
                return new DataWrapper(this.deepCopy(page), this.pageVersions.get(pageIndex));
            }
        }finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try{
            ByteBuffer existing = this.pageCache.get(pageIndex);
            if (existing != null) {
                this.accessCounts.merge(pageIndex, 1, Integer::sum);
                return new DataWrapper(this.deepCopy(existing), this.pageVersions.get(pageIndex));
            }

            ByteBuffer loadedPage = loader.load(pageIndex);
            if (loadedPage == null) throw new StorageOperationException("Page " + pageIndex + " not found");
            if(this.pageCache.size() >= this.maxPage){
                this.evictPage();
            }
            this.pageCache.put(pageIndex, loadedPage);
            this.accessCounts.merge(pageIndex, 1, Integer::sum);
            this.pageVersions.put(pageIndex, 0);
            return new DataWrapper(this.deepCopy(loadedPage), this.pageVersions.get(pageIndex));
        }finally {
            lock.writeLock().unlock();
        }
    }

    public boolean modifyPage(int pageIndex, DataWrapper data){
        ReentrantReadWriteLock lock = this.pageLocks.get(pageIndex);
        if (lock == null) throw new StorageOperationException("Page lock not found");

        lock.writeLock().lock();
        try{
            ByteBuffer page = this.pageCache.get(pageIndex);
            if (page == null) return false;
            int versionBefore = this.pageVersions.get(pageIndex);
            if (versionBefore != data.getVersion()) return false;
            this.pageCache.put(pageIndex, data.getPage());
            this.pageVersions.put(pageIndex, versionBefore + 1);
            this.markDirty(pageIndex);
            return  true;
        }finally {
            lock.writeLock().unlock();
        }

    }

    public int allocatePage(ByteBuffer page){
        headerLock.lock();
        try {
            HeaderPage header = this.loadHeaderPage();
            int freePageIndex = header.getFirstFreePage();

            if (freePageIndex != -1) {
                FreePage freePage = new FreePage(loader.load(freePageIndex), freePageIndex);
                int nextFreeIndex = freePage.getNextFreePage();
                header.setFirstFree(nextFreeIndex);
                this.markDirty(0);
                this.cachePage(nextFreeIndex, page);
                return nextFreeIndex;
            } else {
                int pageIndex = header.getTotalPage();
                header.setTotalPage(pageIndex + 1);
                this.markDirty(0);
                this.cachePage(pageIndex, page);
                this.saver.expandFileIfNeeded(header.getTotalPage());
                return pageIndex;
            }
        } finally {
            headerLock.unlock();
        }
    }

    public void deletePage(int pageIndex) {
        headerLock.lock();
        try {
            HeaderPage header = this.loadHeaderPage();
            int freePageIndex = header.getFirstFreePage();
            FreePage freePage = new FreePage(pageIndex);
            freePage.setNextFreePage(freePageIndex);

            boolean modified = false;
            int attempts = 0;
            while (!modified && attempts < 5) { // Ограничиваем количество попыток
                DataWrapper pageToDelete = this.loadPage(pageIndex);
                pageToDelete.replacePage(freePage.getData());
                modified = this.modifyPage(pageIndex, pageToDelete);
                attempts++;
            }
            if (!modified) {
                throw new StorageOperationException("Failed to delete page after multiple attempts.");
            }

            header.setFirstFree(pageIndex);
            this.markDirty(0);
        } finally {
            headerLock.unlock();
        }
    }



    private void cachePage(int pageIndex, ByteBuffer page) {
        pageLocks.putIfAbsent(pageIndex, new ReentrantReadWriteLock());
        ReentrantReadWriteLock lock = pageLocks.get(pageIndex);
        lock.writeLock().lock();
        try {
            if (pageCache.size() >= maxPage) {
                evictPage();
            }
            pageCache.put(pageIndex, page);
            accessCounts.put(pageIndex, 1);
            pageVersions.putIfAbsent(pageIndex, 0);
        } finally {
            lock.writeLock().unlock();
        }
    }


    public void markDirty(int pageIndex) {
        this.accessCounts.merge(pageIndex, 1, Integer::sum);
        this.dirtyPages.add(pageIndex);
    }

//    public void flushPage(int pageIndex) {
//        this.pageLocks.putIfAbsent(pageIndex, new ReentrantReadWriteLock());
//        ReentrantReadWriteLock lock = this.pageLocks.get(pageIndex);
//
//        lock.writeLock().lock();
//        try{
//            ByteBuffer page = this.pageCache.get(pageIndex);
//            if(page != null && this.dirtyPages.contains(pageIndex)){
//                this.saver.save(pageIndex, page);
//                this.dirtyPages.remove(pageIndex);
//            }
//        }  finally{
//            lock.writeLock().unlock();
//        }
//    }

    public void flushPage(int pageIndex) {
        this.pageLocks.putIfAbsent(pageIndex, new ReentrantReadWriteLock());
        ReentrantReadWriteLock lock = this.pageLocks.get(pageIndex);

        lock.writeLock().lock();
        ByteBuffer pageCopy = null;
        boolean isDirty = false;
        try {
            ByteBuffer page = this.pageCache.get(pageIndex);
            if (page != null && this.dirtyPages.contains(pageIndex)) {
                pageCopy = deepCopy(page);
                isDirty = true;
                this.dirtyPages.remove(pageIndex);
            }
        } finally {
            lock.writeLock().unlock();
        }

        if (isDirty) {
            this.saver.save(pageIndex, pageCopy);
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
        Integer pageIndexToRemove;

        pageIndexToRemove = accessCounts.entrySet().stream()
                .filter(e -> !e.getKey().equals(0))
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


    private ByteBuffer deepCopy(ByteBuffer original) {
        ByteBuffer copy = ByteBuffer.allocate(original.capacity());
        int oldPos = original.position();
        int oldLimit = original.limit();
        copy.put(original.duplicate());
        original.position(oldPos).limit(oldLimit);
        return copy;
    }


    private HeaderPage loadHeaderPage(){
        return new HeaderPage(this.loadPage(0).getPage(), 0);
    }


    interface PageLoader { ByteBuffer load(int pageIndex);}
    interface PageSaver {void save(int pageIndex, ByteBuffer data); void expandFileIfNeeded(int totalPage);}

}
