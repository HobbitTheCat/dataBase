package TableManager;

import NewQuery.Condition;
import TableManager.Exceptions.TableAlreadyExistException;
import TableManager.Exceptions.TableManagementException;
import PageManager.PageManager;
import Pages.*;

import java.lang.reflect.Array;
import java.util.*;

public class TableManager {
    /**
     *  Constructor
     */
    private final PageManager pageManager;
    public TableManager(PageManager pageManager) {
        this.pageManager = pageManager;
    }

    /**
     * Table lookup functions
     */

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
        if(metaPage == null) throw new TableManagementException("Data base file is not properly created");
        TableDescription result = this.searchTable(tableName, metaPage);
        if(result == null) throw new TableManagementException("Table with provided name is not found");
        this.releasePage(metaPage);
        return result;
    }

    /**
     *  Function for finding free space on the objects page for an object or allocating a new one
     */
    private Address insertToFreeObjectPlace(ObjectPage objectPage){
        int index = objectPage.allocate();
        if (index > -1) return new Address(objectPage.getPageNumber(), index);

        while (objectPage.getNextPage() != -1) {
            ObjectPage tempObjectPage = (ObjectPage) this.acquirePage(objectPage.getNextPage());
            this.releasePage(objectPage);
            objectPage = tempObjectPage;
            if ((index = objectPage.allocate()) > -1) return new Address(objectPage.getPageNumber(), index);
        }

        FreePage newPage = (FreePage) this.acquirePage(-1);
        if(newPage == null) throw new TableManagementException("Page can't be allocated");
        ObjectPage newObjectPage = new ObjectPage(objectPage.getObjectLength(), newPage.getPageNumber());
        objectPage.setNextPage(newObjectPage.getPageNumber());
        this.pageManager.exchangePage(newPage, newObjectPage);
        return  new Address(newPage.getPageNumber(), newObjectPage.allocate());
    }

    private void insertToFreeMetaPlace(MetaPage metaPage, TableDescription newTable){
        int offset = metaPage.add(newTable);
        if(offset > -1) return;

        while (metaPage.getNextPage() != -1) {
            metaPage = (MetaPage) this.acquirePage(metaPage.getNextPage());
            if((offset = metaPage.add(newTable)) > -1) return;
        }

        FreePage newPage = (FreePage) this.acquirePage(-1);
        if(newPage == null) throw new TableManagementException("Page can't be allocated");
        MetaPage newMetaPage = new MetaPage(newPage.getPageNumber());
        metaPage.setNextPage(newPage.getPageNumber());
        this.pageManager.exchangePage(newPage, newMetaPage);
        offset = newMetaPage.add(newTable);
        if(offset == -1) throw new TableManagementException("Object description is to long to be  inserted");
    }

    private Address insertIntoStringPage(StringPage page, String stringToInsert, Address backAddress){
        short index = page.add(stringToInsert, backAddress);
        if(index > -1) return new Address(page.getPageNumber(), index);
        while (page.getNextPage() != -1) {
            StringPage tempPage = (StringPage) this.acquirePage(page.getNextPage());
            this.releasePage(page);
            page = tempPage;
            if((index = page.add(stringToInsert, backAddress) )> -1) return new  Address(page.getPageNumber(), index);
        }

        FreePage newPage = (FreePage) this.acquirePage(-1);
        if(newPage == null) throw new TableManagementException("Page can't be allocated");
        StringPage newStringPage = new StringPage(newPage.getPageNumber());
        page.setNextPage(newStringPage.getPageNumber());
        this.pageManager.exchangePage(newPage, newStringPage);
        return new Address(newStringPage.getPageNumber(), newStringPage.add(stringToInsert, backAddress));
    }

    /**
     * Value search functions on pages
     */
    private Address[] searchForAddressesOnStringPage(StringPage firstPage, String stringToSearch) {
        ArrayList<Address> returnAddresses = new ArrayList<>();
        StringPage page = firstPage;

        while (page != null) {
            Collections.addAll(returnAddresses, page.searchString(stringToSearch));
            int nextPageIndex = page.getNextPage();
            page = (nextPageIndex != -1) ? (StringPage) this.acquirePage(nextPageIndex) : null;
        }

        return returnAddresses.toArray(new Address[0]);
    }

    /**
     * Function for creating a new table
     * for creation we need to get class name, all attributes names, attributes types for creating right pages
     * @param newTable object describing a new table
     */
    public void createTable(TableDescription newTable) {
        try {
            MetaPage metaPage = (MetaPage) this.acquirePage(1);
            if (metaPage == null) throw new TableManagementException("Data base file is not properly created");

            if (this.searchTable(newTable.getName(), metaPage) != null)
                throw new TableAlreadyExistException("Table already exists");
            FreePage newPage = (FreePage) this.acquirePage(-1);
            if (newPage == null) throw new TableManagementException("Object page can't be allocated");
            ObjectPage objectPage = new ObjectPage((short) newTable.getAttributeNumber(), newPage.getPageNumber());
            this.pageManager.exchangePage(newPage, objectPage);
            newTable.setObjectPage(objectPage.getPageNumber());
            int[] newPages = new int[newTable.getAttributeNumber()];
            Arrays.fill(newPages, -1);
            List<Page> pages = this.acquirePage(newPages);
            if (pages.size() < newTable.getAttributeNumber()) {
                List<Integer> pageList = new ArrayList<>();
                for (Page page : pages) pageList.add(page.getPageNumber());
                this.pageManager.deletePages(pageList);
                throw new TableManagementException("Attribute page can't be allocated");
            }
            for (int i = 0; i < newTable.getAttributeNumber(); i++) {
                int pageNumber = pages.get(i).getPageNumber();
                switch (newTable.getAttributesTypes()[i]) {
                    case "string":
                    case "String":
                        Page oldPage = pages.get(i);
                        StringPage stringPage = new StringPage(pageNumber);
                        this.pageManager.exchangePage(oldPage, stringPage);
                        break;
                    default:
                        throw new TableManagementException("Unknown attribute type: " + newTable.getAttributesTypes()[i]);
                }
                newTable.setAttributesPage(i, pageNumber);
            }

            try {
                this.insertToFreeMetaPlace(metaPage, newTable);
            } catch (TableManagementException e) {
                List<Integer> pageList = new ArrayList<>();
                for (Page page : pages) pageList.add(page.getPageNumber());
                this.pageManager.deletePages(pageList);
                throw e;
            }
        } finally {
            this.pageManager.releaseAllPages();
        }
    }

    public void createTableIfNotExist(TableDescription newTable){
        try{
            this.createTable(newTable);
        } catch(TableAlreadyExistException e){
            return;
        }
    }

    /**
     * Function for adding an object to an existing table
     * Here we need class name, attributes names and values and also types
     * @param newTable description of which table the object should be inserted into
     * @param attributesValues new object values
     */

    public void addObject(TableDescription newTable, Object[] attributesValues) {
        try {
            TableDescription tableOfObject = this.getTableByNameWithRelease(newTable.getName()); //get description of target table

            int[] pageNeeded = new int[attributesValues.length];
            for (int i = 0; i < newTable.getAttributeNumber(); i++) {
                int internalIndex = tableOfObject.getAttributeInternalIndexByName(newTable.getAttributesNames()[i]);
                if (internalIndex == -1)
                    throw new TableManagementException("Attribute " + newTable.getAttributesNames()[i] + " is not defined in the table");
                pageNeeded[i] = tableOfObject.getAttributePageByName(newTable.getAttributesNames()[i]);
            } // check that all passed attributes names actually exists

            ObjectPage pageOfObjects = (ObjectPage) this.acquirePage(tableOfObject.getObjectPage());
            if (pageOfObjects == null) throw new TableManagementException("Table is not properly defined");
            Address addressOfNewObject = this.insertToFreeObjectPlace(pageOfObjects); // back address

            Address[] addresses = new Address[tableOfObject.getAttributeNumber()];
            List<Page> acquiredPages = this.acquirePage(pageNeeded);

            if(acquiredPages.size() < pageNeeded.length) { //error
                ObjectPage objectPage = (ObjectPage) this.acquirePage(addressOfNewObject.getPageNumber());
                objectPage.delete(addressOfNewObject.getOffset());
                throw new TableManagementException("Attribute page can't be allocated");
            }

            Map<Integer, Page> acquiredPagesMap = new HashMap<>(acquiredPages.size()); //map -> order ensured
            for (Page page : acquiredPages) {
                acquiredPagesMap.put(page.getPageNumber(), page);
            }

            try {
                for (int i = 0; i < attributesValues.length; i++) {
                    Page page = acquiredPagesMap.get(tableOfObject.getAttributePageByName(newTable.getAttributeName(i)));
                    switch (newTable.getAttributeType(i)) {
                        case "string":
                        case "String":
                            StringPage stringPage = (StringPage) page;
                            Address addressOfString = this.insertIntoStringPage(stringPage, (String) attributesValues[i], addressOfNewObject);
                            addresses[tableOfObject.getAttributeInternalIndexByName(newTable.getAttributeName(i))] = addressOfString;
                            break;
                        default:
                            throw new TableManagementException("Unknown attribute type: " + newTable.getAttributeType(i));
                    }
                }
            }  catch (Exception e) {
                throw e;
            }

            for(int i = 0; i < addresses.length; i++){
                if(addresses[i] ==  null) addresses[i] = new Address(-1, -1);
            }

            System.out.println("\n" + addressOfNewObject + "\n");
            ObjectPage objectPage = (ObjectPage) this.acquirePage(addressOfNewObject.getPageNumber());
            objectPage.insertToIndex(addresses, addressOfNewObject.getOffset());

        }finally {this.pageManager.releaseAllPages();}
    }

    /**
     * Function for searching objects in dataBase
     *
     */

    public void searchObject(TableDescription searchVictim, Condition[] conditions) {
        try{
            TableDescription tableOfObject = this.getTableByNameWithRelease(searchVictim.getName()); //get description of target table
            ArrayList<Condition> actualConditions = new ArrayList<>(); // list of condition that can be applied
            for (Condition condition : conditions) {
                if(tableOfObject.getAttributePageByName(condition.attributeName()) != -1)
                    actualConditions.add(condition);
            }

            //applying firs condition




        } finally {this.pageManager.releaseAllPages();}
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
