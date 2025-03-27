package NewQuery;

import java.io.Serializable;

enum command{
    CREATE, READ, UPDATE, DELETE
}

public abstract class Query implements Serializable {
    private command operationType; // type of operation that need's to be done with DB
    private String name; // class name
    private Condition[] conditions; // logical condition ([] if none)
    private String[] attributeNames; // names of attributes of class
    private String[] attributeValues; // value of attributes ([] if none)
    private String[] attributeTypes; // types of attributes  in most cases len(attributeNames) == len(attributeValues) == len(attributeTypes) apart for class creation

    // define getters and setters in mode protected

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

