package TableManager;

import Pages.Interface.BackLinkPage;
import NewQuery.Condition;
import TableManager.Exceptions.TableAlreadyExistException;
import TableManager.Exceptions.TableManagementException;
import PageManager.PageManager;
import Pages.*;
import java.util.*;
import java.util.function.Function;

/**
 * Name of class: TableManager
 * <p>
 * Description: Makes us able to execute actions on the database, mostly complicated stuff.
 * <p>
 * Version: 3.0
 * <p>
 * Date 03/30
 * <p>
 * Copyright: Semenov Egor
 */

public class TableManager {
    /**
     *  Constructor
     */
    private final PageManager pageManager;
    public TableManager(PageManager pageManager) {
        this.pageManager = pageManager;
    }
    private final Map<Integer, Page> acquiredPages = new HashMap<>();

    /**
     * Table lookup functions
     */
    public TableDescription searchTable(String tableName) {
        try {
            MetaPage metaPage = (MetaPage) this.acquirePage(1);
            return this.searchTable(tableName, metaPage);
        } finally {
            this.releaseAllPages();
        }
    }

    private TableDescription searchTable(String tableName, MetaPage metaPage){
        TableDescription table;
        MetaPage pageToUse = metaPage;
        while(pageToUse != null){
            table = pageToUse.getClassByName(tableName);
            if(table != null) return table;
            int nextPageIndex = pageToUse.getNextPage();
            if(pageToUse != metaPage) this.releasePage(pageToUse);
            pageToUse = (nextPageIndex != -1) ? (MetaPage) this.acquirePage(nextPageIndex) : null;
        }
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
            index = objectPage.allocate();
            if (index > -1) return new Address(objectPage.getPageNumber(), index);
        }

        FreePage newFreePage = (FreePage) this.acquirePage(-1);
        if(newFreePage == null) throw new TableManagementException("Page can't be allocated");

        int newPageNumber = newFreePage.getPageNumber();
        ObjectPage newObjectPage = new ObjectPage(objectPage.getObjectLength(), newPageNumber);
        objectPage.setNextPage(newPageNumber);
        this.pageManager.exchangePage(newFreePage, newObjectPage);
        index = newObjectPage.allocate();
        return  new Address(newPageNumber, index);
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

    private <T> Address insertIntoBackLinkPage(BackLinkPage<T> page, T value, Address backAddress, Function<Integer, BackLinkPage<T>> pageCreator){
        short index = page.add(value, backAddress);
        if(index > -1) return new Address(page.getPageNumber(), index);
        while (page.getNextPage() != -1){
            BackLinkPage<T> tempPage = (BackLinkPage<T>) this.acquirePage(page.getNextPage());
            this.releasePage((Page) page);
            page = tempPage;
            index = page.add(value, backAddress);
            if(index > -1) return new Address(page.getPageNumber(), index);
        }

        FreePage newFreePage = (FreePage) this.acquirePage(-1);
        if(newFreePage == null) throw new TableManagementException("Page can't be allocated");

        int newPageNumber = newFreePage.getPageNumber();
        BackLinkPage<T> newPage = pageCreator.apply(newPageNumber);
        page.setNextPage(newPageNumber);
        this.pageManager.exchangePage(newFreePage, (Page) newPage);
        index = newPage.add(value, backAddress);
        return  new Address(newPageNumber, index);
    }
    private Address insertIntoStringPage(StringPage page, String stringToInsert, Address backAddress) {
        return insertIntoBackLinkPage(page, stringToInsert, backAddress,
                pageNumber -> new StringPage(pageNumber));
    }

    private Address insertIntoLongPage(LongPage page, Long longValue, Address backAddress) {
        return insertIntoBackLinkPage(page, longValue, backAddress,
                pageNumber -> new LongPage(pageNumber));
    }


    /**
     * Value search functions on pages
     */
    private ArrayList<Address> searchForAddresses(Page firstPage, Condition condition){
        ArrayList<Address> returnAddresses = new ArrayList<>();
        Page page = firstPage;
        while (page != null) {
            switch (page.getType()) {
                case 2:
                    returnAddresses.addAll(((StringPage) page).search(condition)); break;
                case 3:
                    returnAddresses.addAll(((LongPage) page).search(condition)); break;
                default: throw new TableManagementException("Unknown set type: " + page.getType());
            }
            int nextPageIndex = page.getNextPage();
            page = (nextPageIndex != -1) ? (Page) this.acquirePage(nextPageIndex) : null;
        }
        return returnAddresses;
    }

    /**
     * Function to restore object
     */

    private Map<String, Object> restoreObject(TableDescription classThatWeTryToRestore, Address objectPageAddress){
        ObjectPage objectPage = (ObjectPage) this.acquirePage(objectPageAddress.getPageNumber());
        Address[] addresses = objectPage.get(objectPageAddress.getOffset());

        Map<String, Object> returnMap = new HashMap<>();
        for(int i = 0; i < addresses.length; i++) {
            if (!addresses[i].isNull()) {
                Page oneOfNeededPage = this.acquirePage(addresses[i].getPageNumber());
                switch (oneOfNeededPage.getType()) {
                    case 2:
                        StringPage stringPage = (StringPage) oneOfNeededPage;
                        returnMap.put(classThatWeTryToRestore.getAttributeName(i), stringPage.get(addresses[i].getOffset()));
                        break;
                    case 3:
                        LongPage longPage = (LongPage) oneOfNeededPage;
                        returnMap.put(classThatWeTryToRestore.getAttributeName(i), longPage.get(addresses[i].getOffset()));
                        break;
                    default:
                        throw new TableManagementException("I don't know how page of type " + oneOfNeededPage.getType() + " was found here");
                }
            } else returnMap.put(classThatWeTryToRestore.getAttributeName(i), null);
        }
        return returnMap;
    }

    private ArrayList<Map<String, Object>> restoreAllObjects(TableDescription classThatWeTryToRestore, ObjectPage objectPage) {
        ArrayList<Map<String, Object>> returnList = new ArrayList<>();
        while(objectPage != null){
            Address[] addresses = objectPage.getAllObjectAddresses();
            for(Address address : addresses)
                returnList.add(this.restoreObject(classThatWeTryToRestore, address));
            int nextPageIndex = objectPage.getNextPage();
            objectPage = (nextPageIndex != -1) ?  (ObjectPage) this.acquirePage(nextPageIndex) : null;
        }
        return returnList;
    }

    /**
     * Function to delete object
     */
    private void deleteObject(Address objectPageAddress){
        ObjectPage objectPage = (ObjectPage) this.acquirePage(objectPageAddress.getPageNumber());
        Address[] addresses = objectPage.get(objectPageAddress.getOffset());
        boolean[] alreadyDeleted = new boolean[addresses.length];
        try {
            for (int i = 0; i < addresses.length; i++) {
                if (addresses[i].isNull()) {
                    Page oneOfNeededPage = this.acquirePage(addresses[i].getPageNumber());
                    switch (oneOfNeededPage.getType()) {
                        case 2 -> {
                            StringPage stringPage = (StringPage) oneOfNeededPage;
                            stringPage.delete(addresses[i].getOffset());
                        }
                        case 3 -> {
                            LongPage longPage = (LongPage) oneOfNeededPage;
                            longPage.delete(addresses[i].getOffset());
                        }
                        default ->
                                throw new TableManagementException("I don't know how page of type " + oneOfNeededPage.getType());
                    }
                }
                alreadyDeleted[i] = true;
            } objectPage.delete(objectPageAddress.getOffset());
        } catch (Exception e) {
            for(int i = 0; i < alreadyDeleted.length; i++){
                if(alreadyDeleted[i]){
                    objectPage.replaceAddress(objectPageAddress.getOffset(), i, new Address(-1, -1));
                }
            }
        }
    }

    private void deleteAllObjects(ObjectPage objectPage) {
        while(objectPage != null){
            Address[] addresses = objectPage.getAllObjectAddresses();
            for(Address address : addresses)
                this.deleteObject(address);
            int nextPageIndex = objectPage.getNextPage();
            objectPage =  (nextPageIndex != -1) ? (ObjectPage) this.acquirePage(nextPageIndex) : null;
        }
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
            Page oldPage;
            for (int i = 0; i < newTable.getAttributeNumber(); i++) {
                int pageNumber = pages.get(i).getPageNumber();
                String type = newTable.getAttributesTypes()[i].toLowerCase();
                switch (type) {
                    case "string" -> {
                        oldPage = pages.get(i);
                        StringPage stringPage = new StringPage(pageNumber);
                        this.pageManager.exchangePage(oldPage, stringPage);
                    }
                    case "integer", "int", "long" ->{
                        oldPage = pages.get(i);
                        LongPage longPage = new LongPage(pageNumber);
                        this.pageManager.exchangePage(oldPage, longPage);
                    }
                    default -> throw new TableManagementException("Unknown set type: " + newTable.getAttributesTypes()[i]);
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
            this.releaseAllPages();
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

    public void addObject(TableDescription newTable, Map<String, Object> attributesValues) {
        try {
            TableDescription tableOfObject = this.getTableByNameWithRelease(newTable.getName()); //get description of target table

            int[] pageNeeded = new int[attributesValues.size()];
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
                for (int i = 0; i < attributesValues.size(); i++) {
                    Page page = acquiredPagesMap.get(tableOfObject.getAttributePageByName(newTable.getAttributeName(i)));
                    String attributeType = newTable.getAttributeType(i).toLowerCase();
                    Object value = attributesValues.get(newTable.getAttributeName(i));
                    Address address;
                    if (value == null){ // don't know if it needs to be here
                        address = new Address(-1, -1);
                    } else {
                        switch (attributeType) {
                            case "string" -> {
                                address = this.insertIntoStringPage((StringPage) page, (String) value, addressOfNewObject);
                            }
                            case "integer", "int", "long", "short", "byte" -> { //not sure about double
                                address = this.insertIntoLongPage((LongPage) page, ((Number) value).longValue(), addressOfNewObject);
                            }
                            default -> {
                                throw new TableManagementException("Unknown set type: " + attributeType);
                            }
                        }
                    }
                    addresses[tableOfObject.getAttributeInternalIndexByName(newTable.getAttributeName(i))] = address;
                }
            }  catch (Exception e) {
                // here we need to add rollback code for all changed pages
                throw e;
            }

            for(int i = 0; i < addresses.length; i++){
                if(addresses[i] ==  null) addresses[i] = new Address(-1, -1);
            }

            System.out.println("\n" + addressOfNewObject + "\n");
            ObjectPage objectPage = (ObjectPage) this.acquirePage(addressOfNewObject.getPageNumber());
            objectPage.insertToIndex(addresses, addressOfNewObject.getOffset());

        }finally {this.releaseAllPages();}
    }

    /**
     * Function for searching objects in dataBase
     */

    public ArrayList<Map<String, Object>> searchObject(TableDescription searchVictim, ArrayList<Condition> conditions) {
        try{
            TableDescription tableOfObject = this.getTableByNameWithRelease(searchVictim.getName()); //get description of target table
            ArrayList<Condition> actualConditions = this.sortingApplicableConditions(tableOfObject, conditions);

            if(actualConditions.isEmpty()){
                return this.restoreAllObjects(tableOfObject, (ObjectPage) this.acquirePage(tableOfObject.getObjectPage()));
            }

            //applying firs condition
            Condition fisrtCondition = actualConditions.get(0);
            Page page = this.acquirePage(tableOfObject.getAttributePageByName(fisrtCondition.attributeName()));
            ArrayList<Address> addresses = this.searchForAddresses(page, fisrtCondition);

            ArrayList<Map<String, Object>> objects = new ArrayList<>();
            for(Address address : addresses){
                objects.add(this.restoreObject(tableOfObject, address));
            }

            //now applying all other conditions
            for(int i = 1; i < actualConditions.size(); i++) {
                Condition condition = actualConditions.get(i);
                String attributeName = condition.attributeName();
                Iterator<Map<String, Object>> iterator = objects.iterator();
                while (iterator.hasNext()) {
                    Map<String, Object> obj = iterator.next();
                    String attributeType = searchVictim.getAttributeType(searchVictim.getAttributeInternalIndexByName(attributeName)).toLowerCase();
                    switch (attributeType){
                        case "string" -> {
                            if (!StringPage.applyCondition((String) obj.get(attributeName), condition.operator(), (String) condition.value())) iterator.remove();
                        }
                        case "integer", "int", "long", "short", "byte", "double" -> {
                            if (!LongPage.applyCondition(((Number) obj.get(attributeName)).longValue(), condition.operator(), ((Number)condition.value()).longValue())) iterator.remove();
                        }
                        default -> {
                            throw new TableManagementException("Unknown set type: " + attributeType);
                        }
                    }
                }
            }
            return objects;
        } finally {this.releaseAllPages();}
    }

    // private functions applicable to the conditions
    private ArrayList<Condition> sortingApplicableConditions(TableDescription tableOfObject,  ArrayList<Condition> conditions) {
        ArrayList<Condition> actualConditions = new ArrayList<>(); // list of condition that can be applied
        for (Condition condition : conditions) {
            if(tableOfObject.getAttributePageByName(condition.attributeName()) != -1)
                actualConditions.add(condition);
        }
        return actualConditions;
    }

    /**
     * Function for deleting objects
     */
    public void deleteObject(TableDescription deleteVictim, ArrayList<Condition> conditions) {
        try{
            TableDescription tableOfObject = this.getTableByNameWithRelease(deleteVictim.getName()); //get description of target table
            ArrayList<Condition> actualConditions = this.sortingApplicableConditions(tableOfObject, conditions);

            if(actualConditions.isEmpty()){
                this.deleteAllObjects((ObjectPage) this.acquirePage(tableOfObject.getObjectPage()));
                return;
            }

            Condition fisrtCondition = actualConditions.get(0);
            Page page = this.acquirePage(tableOfObject.getAttributePageByName(fisrtCondition.attributeName()));
            ArrayList<Address> addresses = this.searchForAddresses(page, fisrtCondition);

            ArrayList<Address> addressesToDelete = new ArrayList<>();
            ArrayList<Map<String, Object>> objects = new ArrayList<>();
            for(Address address : addresses){
                objects.add(this.restoreObject(tableOfObject, address));
                addressesToDelete.add(address);
            }

            for(int i = 1; i < actualConditions.size(); i++) {
                Condition condition = actualConditions.get(i);
                String attributeName = condition.attributeName();
                Iterator<Map<String, Object>> iterator = objects.iterator();
                Iterator<Address>  iteratorToDelete = addressesToDelete.iterator();
                while (iterator.hasNext()) {
                    Map<String, Object> obj = iterator.next();
                    String attributeType = deleteVictim.getAttributeType(deleteVictim.getAttributeInternalIndexByName(attributeName)).toLowerCase();
                    switch (attributeType){
                        case "string" -> {
                            if (!StringPage.applyCondition((String) obj.get(attributeName), condition.operator(), (String) condition.value())) {
                                iterator.remove();
                                iteratorToDelete.remove();
                            }
                        }
                        case "integer", "int", "long", "short", "byte", "double" -> {
                            if (!LongPage.applyCondition(((Number) obj.get(attributeName)).longValue(), condition.operator(), ((Number)condition.value()).longValue())) {
                                iterator.remove();
                                iteratorToDelete.remove();
                            }
                        }
                        default -> {
                            throw new TableManagementException("Unknown set type: " + attributeType);
                        }
                    }
                }
            }

            for(Address address : addressesToDelete){
                this.deleteObject(address);
            }

        }finally {
            this.releaseAllPages();
        }
    }

    public void deleteTable(TableDescription deleteVictim) {
        try{
            TableDescription tableOfObject = this.getTableByNameWithRelease(deleteVictim.getName()); //get description of target table
            String[] attributes = tableOfObject.getAttributesNames();
            ArrayList<Integer> pagesToDelete = new ArrayList<>(this.getAllPagesToDelete(tableOfObject.getObjectPage()));
            for(String attributeName : attributes){
                pagesToDelete.addAll(this.getAllPagesToDelete(tableOfObject.getAttributePageByName(attributeName)));
            }
            this.pageManager.deletePages(pagesToDelete);
            this.deleteTableFromMetaPage(deleteVictim.getName());
        } finally {
            this.releaseAllPages();
        }
    }

    private void deleteTableFromMetaPage(String tableName){
        MetaPage metaPage = (MetaPage) this.acquirePage(1);
        while(metaPage !=  null){
            if(metaPage.deleteClassByName(tableName)) return;
            int nextPageIndex = metaPage.getNextPage();
            metaPage = (nextPageIndex != -1) ? (MetaPage) this.acquirePage(nextPageIndex) : null;
        }
    }

    private ArrayList<Integer> getAllPagesToDelete(int firstPageIndex){
        ArrayList<Integer> pagesToDelete = new ArrayList<>();
        Page page = this.acquirePage(firstPageIndex);
        while(page != null){
            pagesToDelete.add(page.getPageNumber());
            int nextPageIndex = page.getNextPage();
            page = (nextPageIndex != -1) ? this.acquirePage(nextPageIndex) : null;
        }
        return pagesToDelete;
    }

    /**
     * Additional functions
     */
    private <T extends Page> T accurePageTest(int pageNumber){
        List<Page> result = this.acquirePage(new int[]{pageNumber});
        return (T) result.get(0);
    }

    private Page acquirePage(int pageNumber){
        List<Page> result = this.acquirePage(new int[]{pageNumber});
        return result.get(0);
    }
    private List<Page> acquirePage(int[] pageNumber){
        List<Integer> pageList = new ArrayList<>();
        List<Page> result = new ArrayList<>();
        for(int number: pageNumber){
            Page page = this.acquiredPages.get(number);
            if(page != null) result.add(page);
            else pageList.add(number);
        }
        if(this.acquiredPages.isEmpty()) { // this.pageManager.getHeldResources().isEmpty() take graph lock, suboptimal
            try {
                result.addAll(this.pageManager.acquireResources(pageList));
                return result;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                result.addAll(this.pageManager.expandResourceZone(pageList));
                return result;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>();
    }

    private void releasePage(Page page){
        this.acquiredPages.remove(page.getPageNumber());
        this.pageManager.releasePages(List.of(page.getPageNumber()));
    }

    private void releaseAllPages(){
        this.acquiredPages.clear();
        this.pageManager.releaseAllPages();
    }


}
