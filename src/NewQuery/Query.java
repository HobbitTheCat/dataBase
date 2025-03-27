package NewQuery;

import java.io.Serializable;

public abstract class Query implements Serializable {
    private command operationType; // type of operation that need's to be done with DB
    private String name; // class name
    private Condition[] conditions; // logical condition ([] if none)
    private String[] attributeNames; // names of attributes of class
    private Object[] attributeValues; // value of attributes ([] if none)
    private String[] attributeTypes; // types of attributes  in most cases attributeNames.length == attributeValues.length == attributeTypes.length apart for class creation
    //                  └> in case of class creation (attributeNames.length == 0) must be not empty

    // define getters and setters in mode public
    public command getOperationType() {return this.operationType;}
    public String getName() {return this.name;}
    public String[] getAttributeNames() {return this.attributeNames;}
    public Object[] getAttributeValues() {return this.attributeValues;}
    public String[] getAttributeTypes() {return this.attributeTypes;}

    public static <T> SelectQuery select(Class<T> entityClass) {
        //save class name
        return new SelectQuery();
    }

    public static <T> DeleteQuery delete(Class<T> entityClass){
        // save class name
        return new DeleteQuery();
    }

    public static <T> CreateQuery create(Class<T> entityClass){
        // save class name
        return new CreateQuery();
    }

    public static <T> UpdateQuery update(Class<T> entityClass){
        //save class name
        return new UpdateQuery();
    }
}

