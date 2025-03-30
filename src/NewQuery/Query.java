package NewQuery;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Query implements Serializable {
    private command operationType; // type of operation that need's to be done with DB - enum CREATE, READ, UPDATE, DELETE
    private String name; // class name
    private ArrayList<Condition> conditions; // logical condition ([] if none)
    private ArrayList<String> attributeNames; // names of attributes of class
    private Map<String, Object> attributeValues; // value of attributes ([] if none)
    private Map<String, String> attributeTypes; // types of attributes  in most cases attributeNames.length == attributeValues.length == attributeTypes.length apart for class creation


    // define getters and setters in mode public
    public command getOperationType() {return this.operationType;}
    public String getName() {return this.name;}
    public ArrayList<String> getAttributeNames() {return this.attributeNames;}
    public Object getAttributeValueByName(String name) {return this.attributeValues.get(name);}
    public String getAttributeTypeByName(String name) {return this.attributeTypes.get(name);}
    public ArrayList<Condition> getConditions() {return this.conditions;}

    protected void addCondition(Condition condition){
        this.conditions.add(condition);
    }
    protected void setFieldTypes(Map<String, String> fieldTypes) {this.attributeTypes = fieldTypes;}
    protected void setClassName(String name){
        this.name = name;
    }
    protected void addAttributeNames(String[] attributeNames){
        this.attributeNames.addAll(List.of(attributeNames));
    }

    public static <T> SelectQuery select(Class<T> entityClass) {
        return new SelectQuery(entityClass.getName(), getFieldTypesMap(entityClass));
    }

    public static <T> DeleteQuery delete(Class<T> entityClass){
        return new DeleteQuery(entityClass.getName(), getFieldTypesMap(entityClass));
    }

    public static <T> CreateQuery create(Class<T> entityClass){
        return new CreateQuery(entityClass.getName(), getFieldTypesMap(entityClass));
    }

    public static <T> UpdateQuery update(Class<T> entityClass){
        return new UpdateQuery(entityClass.getName(), getFieldTypesMap(entityClass));
    }

    public static Map<String, String> getFieldTypesMap(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .collect(Collectors.toMap(Field::getName, field -> field.getType().getSimpleName()));
    }
}

