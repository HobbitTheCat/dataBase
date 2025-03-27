package TableManager;

import Exceptions.TableManagementException;
import PageManager.PageManager;
import Pages.*;

import java.util.*;

public class TableManager {
    private final PageManager pageManager;
    public TableManager(PageManager pageManager) {
        this.pageManager = pageManager;
    }


    public TableDescription searchForTable(String tableName){
        MetaPage metaPage = (MetaPage) this.acquirePage(1);
        return this.searchTable(tableName, metaPage);
    }

    private TableDescription searchTable(String tableName, MetaPage metaPage){
        TableDescription table;
        MetaPage pageToUse = metaPage;
        do{
            table = pageToUse.getClassByName(tableName);
            if (table != null) return table;
            int nextPage = pageToUse.getNextPage();
            if(pageToUse != metaPage) this.releasePage(metaPage);
            if(nextPage == -1) return null;
            pageToUse = (MetaPage) this.acquirePage(nextPage);
            if(pageToUse == null) return null;
        }while(pageToUse.getNextPage() != -1);
        return null;
    }

    private TableDescription getTableByNameWithRelease(String tableName){
        MetaPage metaPage = (MetaPage) this.acquirePage(1);
        TableDescription result = this.searchTable(tableName, metaPage);
        assert metaPage != null;
        this.releasePage(metaPage);
    }

    public void createTable(TableDescription newTable) {
        MetaPage metaPage = (MetaPage) this.acquirePage(1);

        if(this.searchTable(newTable.getName(),  metaPage) != null) throw new TableManagementException("Table already exists");
        Page objectPage = this.acquirePage(-1);
        if (objectPage == null) throw new TableManagementException("Object page can't be allocated");
        objectPage = new ObjectPage((short) newTable.getAttributeNumber(), objectPage.getPageNumber());
        newTable.setObjectPage(objectPage.getPageNumber());

        int[] newPages = new int[newTable.getAttributeNumber()];
        Arrays.fill(newPages, -1);
        List<Page> pages= this.acquirePage(newPages);
        if(pages.size() <  newTable.getAttributeNumber()) throw new TableManagementException("Attribute page can't be allocated");
        for(int i = 0 ; i < newTable.getAttributeNumber(); i++){
            int pageNumber = pages.get(i).getPageNumber();
            switch (newTable.getAttributesTypes()[i]){
                case "string":
                case "String": pages.set(i, new StringPage(pageNumber)); break;
                default: throw new TableManagementException("Unknown attribute type: " + newTable.getAttributesTypes()[i]);
            }
            newTable.setAttributesPage(i, pageNumber);
        }

        int offset;
        try{
            do {
                assert metaPage != null;
                offset = metaPage.add(newTable);
                if (offset == -1) {
                    int nextPage = metaPage.getNextPage();
                    this.releasePage(metaPage);
                    metaPage = (MetaPage) this.acquirePage(nextPage);
                    if (nextPage == -1) {
                        metaPage = new MetaPage(nextPage);
                    }
                }
            } while(offset == -1);
        } catch(IllegalArgumentException e){e.printStackTrace();}

        this.pageManager.releaseAllPages();
    }

    public void addObject(TableDescription newTable, Object[] attributesValues) {

        
    }

    /**
     * Additional functions
     */
    private Page acquirePage(int pageNumber){
        List<Page> result = this.acquirePage(new int[]{pageNumber});
        if(result == null) return null;
        return result.get(0);
    }
    private List<Page> acquirePage(int[] pageNumber){
        List<Integer> pageList = new ArrayList<>();
        for(int number: pageNumber) pageList.add(number);
        if(this.pageManager.getHeldResources().isEmpty()) {
            try {
                return this.pageManager.acquireResources(pageList);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                return this.pageManager.expandResourceZone(pageList);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>();
    }

    private void releasePage(Page page){
        this.pageManager.releasePages(List.of(page.getPageNumber()));
    }


}
