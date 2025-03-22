package PageManager;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VirtualMemoryManager {
    private final int maxPage;
    private final PageSaver saver;
    private final PageLoader loader;

    private final Map<Integer, ByteBuffer> pageCache;
    private final Set<Integer> dirtyPages;
    private final Map<Integer, Integer> accessCounts = new HashMap<>(); //LFU strategy

    public VirtualMemoryManager(int maxPage, PageSaver saver, PageLoader loader) {
        this.maxPage = maxPage;
        this.pageCache = new HashMap<>(maxPage);
        this.dirtyPages = Collections.newSetFromMap(new HashMap<>());
        this.saver = saver;
        this.loader = loader;
    }

    public ByteBuffer loadPage(int pageIndex) {
        ByteBuffer page = this.pageCache.get(pageIndex);
        if(page != null){
            this.accessCounts.merge(pageIndex, 1, Integer::sum);
            return page.duplicate();
        }else{
            page = this.loader.load(pageIndex);
            if(this.pageCache.size() >= this.maxPage){
                this.evictPage();
            }
            this.pageCache.put(pageIndex, page);
            this.accessCounts.put(pageIndex, 1);
        }
            return page.duplicate();
    }

    public void markDirty(int pageIndex) {
        this.accessCounts.merge(pageIndex, 1, Integer::sum);
        this.dirtyPages.add(pageIndex);
    }

    public void flushPage(int pageIndex) {
        ByteBuffer page = this.pageCache.get(pageIndex);
        if(page != null && this.dirtyPages.contains(pageIndex)){
            this.saver.save(pageIndex, page);
            this.dirtyPages.remove(pageIndex);
        }
    }

    public void flushAll() {
        for(int pageIndex : this.dirtyPages){
            this.flushPage(pageIndex);
        }
    }

    public void unloadPage(int pageIndex) {
        // here it need to ask ResourceManager if resource is free
        this.pageCache.remove(pageIndex);
        this.dirtyPages.remove(pageIndex);
        this.accessCounts.remove(pageIndex);
    }

    private void evictPage() {
        // here also it need to ask ResourceManager if resource is free
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

    interface PageLoader { ByteBuffer load(int pageIndex);}
    interface PageSaver {void save(int pageIndex, ByteBuffer data);}
}
