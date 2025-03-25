package PreviousMemoryManagers;

import Pages.FreePage;
import Pages.HeaderPage;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class VirtualMemoryManagerV3_0 {
    private final int pageSize = 4096;
    private final int maxPage;
    private final PageSaver saver;
    private final PageLoader loader;

    private final Map<Integer, Resource> pageCache;
    private final Set<Integer> dirtyPages;
    private final Map<Integer, Integer> accessCounts = new HashMap<>(); //LFU strategy
    private final ReentrantLock headerLock = new ReentrantLock();


    public VirtualMemoryManagerV3_0(int maxPage, PageSaver saver, PageLoader loader) {
        this.maxPage = maxPage;
        this.pageCache = new HashMap<>(maxPage);
        this.dirtyPages = Collections.newSetFromMap(new HashMap<>());
        this.saver = saver;
        this.loader = loader;
    }

    public Resource loadPage(int pageIndex) {
        if(pageIndex >= this.maxPage) return null;
        Resource page = this.pageCache.get(pageIndex);
        if(page != null){
            this.accessCounts.merge(pageIndex, 1, Integer::sum);
            return page;
        }else{
            synchronized (this.pageCache) {
                page = this.pageCache.get(pageIndex);
                if(page != null){
                    this.accessCounts.merge(pageIndex, 1, Integer::sum);
                    return page;
                }else{
                    page = new Resource(pageIndex, this.loader.load(pageIndex));
                    if(this.pageCache.size() >= this.maxPage){
                        this.evictPage();
                    }
                    this.pageCache.put(pageIndex, page);
                    this.accessCounts.put(pageIndex, 1);
                }
                return page;
            }
        }
    }

    public Resource allocatePage() {
        this.headerLock.lock();
        try{
            HeaderPage header = this.loadHeaderPage();
            int freePageIndex = header.getFirstFreePage();

            if (freePageIndex != -1) {
                FreePage freePage = new FreePage(loader.load(freePageIndex), freePageIndex);
                int nextFreePageIndex = freePage.getNextFreePage();
                header.setFirstFree(nextFreePageIndex);
                this.markDirty(0);
                Resource resource = new Resource(freePageIndex, freePage.getData());
                this.cachePage(nextFreePageIndex, resource);
                return resource;
            } else {
                int pageIndex = header.getTotalPage();
                header.setTotalPage(++pageIndex);
                this.markDirty(0);
                Resource resource = new Resource(pageIndex, ByteBuffer.allocate(this.pageSize));
                this.cachePage(pageIndex, resource);
                this.saver.expandFileIfNeeded(header.getTotalPage());
                return resource;
            }
        }finally {
            this.headerLock.unlock();
        }
    }

    public void deletePage(Resource resource) {
        this.headerLock.lock();
        try{
            HeaderPage header = this.loadHeaderPage();
            int freePageIndex = header.getFirstFreePage();
            FreePage freePage = new FreePage(resource.getId());
            freePage.setNextFreePage(freePageIndex);

            resource.setData(freePage.getData());
            header.setFirstFree(resource.getId());
            this.markDirty(resource.getId());
        } finally {
            this.headerLock.unlock();
        }
    }

    private void cachePage(int pageIndex, Resource page) { //here probably need to be a lock
        if (pageCache.size() >= this.maxPage) this.evictPage();
        this.pageCache.put(pageIndex, page);
        this.accessCounts.put(pageIndex, 1);
    }

    public void markDirty(int pageIndex) {
        this.accessCounts.merge(pageIndex, 1, Integer::sum);
        this.dirtyPages.add(pageIndex);
    }

    public void flushPage(int pageIndex) {
        Resource page = this.pageCache.get(pageIndex);
        if(page != null && this.dirtyPages.contains(pageIndex)){
            this.saver.save(pageIndex, page.getData());
            this.dirtyPages.remove(pageIndex);
        }
    }

    public void flushAll() {
        for(int pageIndex : this.dirtyPages){
            this.flushPage(pageIndex);
        }
    }

    public void unloadPage(int pageIndex) {
        // here it need to ask PreviousMemoryManagers.ResourceManager if resource is free
        this.pageCache.remove(pageIndex);
        this.dirtyPages.remove(pageIndex);
        this.accessCounts.remove(pageIndex);
    }

    private void evictPage() {
        // here also it need to ask PreviousMemoryManagers.ResourceManager if resource is free
        Integer pageIndexToRemove = this.accessCounts.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (pageIndexToRemove == null) return;

        if (this.dirtyPages.contains(pageIndexToRemove)) {
            this.flushPage(pageIndexToRemove);
        }
        this.pageCache.remove(pageIndexToRemove);
        this.dirtyPages.remove(pageIndexToRemove);
        this.accessCounts.remove(pageIndexToRemove);
    }

    private void ageAccessCounts() {
        this.accessCounts.replaceAll((pageIndex, count) -> count / 2);
    }

    private HeaderPage loadHeaderPage() {
        return new HeaderPage(this.loadPage(0).getData(), 0);
    }

    interface PageLoader { ByteBuffer load(int pageIndex);}
    interface PageSaver {void save(int pageIndex, ByteBuffer data);void expandFileIfNeeded(int totalPage);}
}
