package PageManager;

import Pages.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Name of class: MemoryManager
 * <p>
 * Description: Implements a swap strategy (LFU) in the virtual memory, supports the concept of dirty pages, as long as the allocation and deallocation of resouces.
 * <p>
 * Version: 2.0
 * <p>
 * Date 03/17
 * <p>
 * Copyright: Semenov Egor, Mathieu Lemain
 */

public class MemoryManager {
    private static final short FREE_PAGE_TYPE = 99;
    private final int pageSize = 4096;
    private final int dataBaseVersion;

    private final int maxPage;
    private final PageSaver saver;
    private final PageLoader loader;

    private final ConcurrentHashMap<Integer, Page> pageCache;
    private final ConcurrentSkipListSet<Integer> dirtyPages = new ConcurrentSkipListSet<>();
    private final ConcurrentHashMap<Integer, Integer> accessCounts = new ConcurrentHashMap<>();//LFU strategy

    // To synchronize allocation process
    private final ReentrantReadWriteLock headerLock = new ReentrantReadWriteLock();
    private final Lock dirtyLock = new  ReentrantLock();
    private final Lock cacheLock = new ReentrantLock();

    public MemoryManager(int maxPage, String filePath) {
        this.maxPage = maxPage;
        this.pageCache = new ConcurrentHashMap<>(maxPage);
        Path path = Paths.get(filePath);
        this.saver = new FilePageSaver(filePath);
        this.loader = new  FilePageLoader(filePath);
        if(Files.exists(path)) {
            HeaderPage header = this.loadHeaderPage();
            this.dataBaseVersion = header.getDataBaseVersion();
            System.out.println("Connecting to existing file");
        } else {
            try {
                Files.createFile(path);
            }catch (IOException e){e.printStackTrace();}
            HeaderPage header = new HeaderPage(0, 1);
            MetaPage meta = new MetaPage(1);
            this.saver.save(0, header.getData());
            this.saver.save(1, meta.getData());
            this.dataBaseVersion = 1;
            System.out.println("Creating new file");
        }
    }

    public List<Page> indexesToPages(List<Integer> requestedPageIndexes) {
        List<Page> pages = new ArrayList<>(requestedPageIndexes.size());
        for (int index : requestedPageIndexes) {
            if(index != -1) pages.add(this.loadPage(index));
            else pages.add(this.allocatePage());
        }
        return pages;
    }

    public void exchangePage(Page newPage) {
        this.cacheLock.lock();
        try{
            this.pageCache.put(newPage.getPageNumber(),  newPage);
        } finally {
            this.cacheLock.unlock();
        } this.markDirty(newPage.getPageNumber());
    }

    private Page loadPage(int pageNumber){
        this.headerLock.readLock().lock();
        try {
            HeaderPage header = this.loadHeaderPage();
            if (pageNumber > header.getTotalPage()) throw new IndexOutOfBoundsException("Page number " + pageNumber + " is out of bounds for size " + header.getPageNumber());
        }finally {this.headerLock.readLock().unlock();}
        return this.loadPageWithoutCheck(pageNumber);
    }

    private Page loadPageWithoutCheck(int pageNumber){
        Page page = this.pageCache.get(pageNumber);
        if(page != null){
            if(page.getType() == FREE_PAGE_TYPE) throw new IllegalArgumentException("Tried to load free page without allocation");
            this.accessCounts.merge(pageNumber, 1, Integer::sum);
            return page;
        } else {
            this.cacheLock.lock();
            try{
                page = this.pageCache.get(pageNumber);
                if(page !=  null){
                    this.accessCounts.merge(pageNumber, 1, Integer::sum);
                } else {
                    ByteBuffer buffer = loader.load(pageNumber);
                    if(buffer.getShort(0) == FREE_PAGE_TYPE) throw new IllegalArgumentException("Tried to load free page without allocation");
                    page = PageFactory.createPage(buffer, pageNumber);
                    this.cachePage(page);
                }
                return page;
            } finally {
                this.cacheLock.unlock();
            }
        }
    }

    private Page allocatePage(){
        this.headerLock.writeLock().lock();
        try {
            this.cacheLock.lock();
            try {
                HeaderPage header = this.loadHeaderPage();
                int freePageIndex = header.getFirstFreePage();
                if (freePageIndex != -1) {
                    FreePage freePage = new FreePage(this.loader.load(freePageIndex), freePageIndex);
                    int nextFreePageIndex = freePage.getNextFreePage();
                    header.setFirstFree(nextFreePageIndex);
                    this.markDirty(0);
                    this.cachePage(freePage);
                    return freePage;
                } else {
                    int pageIndex = header.getTotalPage();
                    header.setTotalPage(++pageIndex);
                    this.markDirty(0);
                    FreePage freePage = new FreePage(pageIndex);
                    this.cachePage(freePage);
                    this.saver.expandFileIfNeeded(header.getTotalPage());
                    return freePage;
                }
            } finally {
                this.cacheLock.unlock();
            }
        } finally {
            this.headerLock.writeLock().unlock();
        }
    }

    public void deletePage(Page page) {
        this.headerLock.writeLock().lock();
        try{
            HeaderPage header = this.loadHeaderPage();
            int freePageIndex = header.getFirstFreePage();
            FreePage freePage = new FreePage(page.getPageNumber());
            freePage.setNextFreePage(freePageIndex);

//            page.setData(freePage.getData()); // не будет работать
            this.exchangePage(freePage);
            header.setFirstFree(freePage.getPageNumber());
            this.markDirty(freePage.getPageNumber());
            this.markDirty(0);
        } finally {
            this.headerLock.writeLock().unlock();
        }
    }

    private void evictPage(){
        this.cacheLock.lock();
        try {
            List<Integer> sortedPages = this.accessCounts.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .toList();

            for (Integer pageIndexToRemove : sortedPages) {
                if (this.pageCache.get(pageIndexToRemove).getOwner() == null) {
                    if (this.dirtyPages.contains(pageIndexToRemove)) {
                        this.flushPage(pageIndexToRemove);
                    }
                    this.pageCache.remove(pageIndexToRemove);
                    this.dirtyPages.remove(pageIndexToRemove);
                    this.accessCounts.remove(pageIndexToRemove);
                    return;
                }
            }
        } finally {this.cacheLock.unlock();}
    }

    public void markDirty(int pageNumber){
        this.dirtyLock.lock();
        try {
            this.dirtyPages.add(pageNumber);
        }finally {this.dirtyLock.unlock();}
    }

    private void cachePage(Page page){
        if(this.pageCache.size() >= this.maxPage){this.evictPage();}
        this.pageCache.put(page.getPageNumber(), page);
        this.accessCounts.put(page.getPageNumber(), 1);
    }

    public void flushPage(int pageIndex) {
        Page page = this.pageCache.get(pageIndex);
        if(page != null && page.getOwner() == null && this.dirtyPages.contains(pageIndex)){
            this.dirtyLock.lock();
            try{
                if(this.dirtyPages.contains(pageIndex)){
                    this.saver.save(pageIndex, page.getData());
                    this.dirtyPages.remove(pageIndex);
                }
            } finally {this.dirtyLock.unlock();}
        }
    }

    public void flushAll() {
        for(int pageIndex : this.dirtyPages){
            this.flushPage(pageIndex);
        }
    }

    private HeaderPage loadHeaderPage() {
        return new HeaderPage(this.loadPageWithoutCheck(0).getData(), 0);
    }


    public interface PageLoader { ByteBuffer load(int pageIndex);}
    public interface PageSaver {void save(int pageIndex, ByteBuffer data);void expandFileIfNeeded(int totalPage);}
}
