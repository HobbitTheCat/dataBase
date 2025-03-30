package TableManager;

import NewQuery.Query;
import NewQuery.Result;

public class QueryToAction {
    private TableManager tableManager;

    public QueryToAction(TableManager tableManager) {this.tableManager = tableManager;}

    public Result queryRun(Query query){
        return switch (query.getOperationType()) {
            case READ -> this.read(query);
            case UPDATE -> this.update(query);
            case DELETE -> this.delete(query);
            case CREATE -> this.insert(query);
        };
    }

    private Result read(Query query){
        TableDescription searchVictim = new TableDescription(query.getName(), query.getAttributeNames().toArray(new String[1]), query.getAttributeTypes());

        return null;
    }

    private Result insert(Query query){
        // function to insert object in existing table or create new table (is query.attributeValues.length == 0)
        TableDescription newTable = new TableDescription(query.getName(), query.getAttributeNames().toArray(new String[1]), query.getAttributeTypes());
        if(query.getAttributeValues().isEmpty()){
            // case of class creation
            try {
                this.tableManager.createTable(newTable);
                return new Result("OK");
            } catch (Exception e) {
                return new Result("ERROR"); //maybe we need to add error status code description
            }
        } else {
            // case of object insertion
            try{
                this.tableManager.addObject(newTable,  query.getAttributeValues());
            } catch(Exception e){
                return new Result("ERROR");
            }
        }
        return null;
    }

    private Result update(Query query){
        return null;
    }

    private Result delete(Query query){
        return null;
    }
}
