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
    public Map<String, Object> getAttributeValues() {return this.attributeValues;}
    public String getAttributeTypeByName(String name) {return this.attributeTypes.get(name);}
    public Map<String, String> getAttributeTypes(){return this.attributeTypes;}
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

    protected boolean conditionIsApplicable(Condition condition) {
        String attributeType = this.attributeTypes.get(condition.attributeName());
        String operator = condition.operator();
        Object value = condition.value();

        if (attributeType == null) {
            throw new IllegalArgumentException("Attribute type not found for: " + condition.attributeName());
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null for condition: " + condition);
        }

        switch (attributeType) {
            case "String":
                if (!(value instanceof String)) {
                    throw new IllegalArgumentException("Invalid value type. Expected String but got: " + value.getClass().getSimpleName());
                }
                if (!(operator.equals("==") || operator.equals("=") || operator.equals("!=") || operator.equals("contains"))) {
                    throw new IllegalArgumentException("Invalid operator for String: " + operator);
                }
                return true;
            case "Integer":
                if (!(value instanceof Integer)) throw new IllegalArgumentException("Invalid value type for Integer.");
            case "Long":
                if (!(value instanceof Long)) throw new IllegalArgumentException("Invalid value type for Long.");
            case "Short":
                if (!(value instanceof Short)) throw new IllegalArgumentException("Invalid value type for Short.");
            case "Byte":
                if (!(value instanceof Byte)) throw new IllegalArgumentException("Invalid value type for Byte.");
            case "Double":
                if (!(value instanceof Double)) throw new IllegalArgumentException("Invalid value type for Double.");
            case "Float":
                if (!(value instanceof Float)) throw new IllegalArgumentException("Invalid value type for Float.");
                if (!(operator.equals("==") || operator.equals(">") || operator.equals("<") || operator.equals(">=") || operator.equals("<=") || operator.equals("!="))) {
                    throw new IllegalArgumentException("Invalid operator for numeric type: " + operator);
                }
                return true;
            default:
                throw new IllegalArgumentException("Unsupported attribute type: " + attributeType);
        }
    }

}

