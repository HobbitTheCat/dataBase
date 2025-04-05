package NewQuery;

import Exceptions.ORMUsageException;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Name of class: Query
 * <p>
 * Description: Common pattern for representing a Query, different types of query inherits from this class
 * <p>
 * Version: 2.0
 * <p>
 * Date 03/13
 * <p>
 * Copyright: Semenov Egor, Lemain Mathieu
 */

public abstract class Query implements Serializable {
    protected final command operationType; // type of operation that need's to be done with DB - enum CREATE, READ, UPDATE, DELETE
    private final String name; // class name
    private modifier modifier;

    private final ArrayList<Condition> conditions = new ArrayList<>(); // logical condition ([] if none)
    private final ArrayList<String> attributeNames = new ArrayList<>(); // names of attributes of class
    private final Map<String, Object> attributeValues = new HashMap<>(); // value of attributes ([] if none)
    private final Map<String, String> attributeTypes; // types of attributes  in most cases attributeNames.length == attributeValues.length == attributeTypes.length apart for class creation

    // define getters and setters in protected public
    public command getOperationType() {return this.operationType;}
    public String getName() {return this.name;}
    public ArrayList<String> getAttributeNames() {return this.attributeNames;}
    public Object getAttributeValueByName(String name) {return this.attributeValues.get(name);}
    public Map<String, Object> getAttributeValues() {return this.attributeValues;}
    public String getAttributeTypeByName(String name) {return this.attributeTypes.get(name);}
    public Map<String, String> getAttributeTypes(){return this.attributeTypes;}
    public ArrayList<Condition> getConditions() {return this.conditions;}
    public boolean applyToAll(){return this.modifier == NewQuery.modifier.ALL;}

    protected void addCondition(Condition condition){ this.conditions.add(condition);}
    protected void addAttributeNames(String attributeName){this.attributeNames.add(attributeName);}
    protected void addAttributeValue(String name, Object value){this.attributeValues.put(name, value);}
    protected void setModifier(){ this.modifier = NewQuery.modifier.ALL;}

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

    protected Query(String className, command operationType, Map<String, String> fieldTypes) {
        this.operationType = operationType;
        this.name = className;
        this.attributeTypes = fieldTypes;
    }

    protected boolean conditionIsApplicable(Condition condition) {
        String attributeType = this.attributeTypes.get(condition.attributeName());
        String operator = condition.operator();
        Object value = condition.value();

        if (attributeType == null)
            throw new IllegalArgumentException("Attribute type not found for: " + condition.attributeName());
        if (value == null)
            throw new IllegalArgumentException("Value cannot be null for condition: " + condition);
        if (!providedTypeConsistentWithValue(attributeType, value))
            throw new IllegalArgumentException("Attribute type " + attributeType + " is not consistent with provided value: " + value + " whose type is: " + value.getClass());
        if (!operationIsApplicableToSelectedType(attributeType, operator))
            throw new IllegalArgumentException("Condition " + operator + " is not applicable for set type: " + attributeType);
        return true;
    }

    private boolean operationIsApplicableToSelectedType(String attributeType, String operator) {
        switch (attributeType){
            case "String" -> {
                return (operator.equals("==") || operator.equals("=") || operator.equals("!=")  || operator.equals("contains"));
            }
            case "Integer", "Byte", "Long", "Short", "Float" -> {
                return (operator.equals("==") || operator.equals(">") || operator.equals("<") || operator.equals(">=") || operator.equals("<=") || operator.equals("!="));
            }
            default -> {
                throw new IllegalArgumentException("Unsupported set type: " + attributeType);
            }
        }
    }

    private boolean providedTypeConsistentWithValue(String attributeType, Object value) {
        switch (attributeType){
            case "String" -> { return value instanceof String;}
            case "Integer", "int" -> { return value instanceof Integer;}
            case "Byte", "byte" -> { return value instanceof Byte;}
            case "Long", "long" -> { return value instanceof Long;}
            case "Short", "short" -> { return value instanceof Short;}
            case "Float", "float" -> { return value instanceof Float;}
            default -> {
                throw new IllegalArgumentException("Unsupported set type: " + attributeType);
            }
        }
    }

    protected boolean providedTypeConsistentWithClassType(String attributeName, Object value) {
        return this.providedTypeConsistentWithValue(this.getAttributeTypeByName(attributeName), value);
    }

    protected boolean hasAttribute(String attributeName) {
        return this.attributeTypes.containsKey(attributeName);
    }

    protected void setAttributeObject(Object object) {
        Class<?> clazz = object.getClass();
        if (!clazz.getName().equals(this.name)) throw new ORMUsageException("Provided class is not the same as the object class");
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = field.getName();

            if (!attributeNames.contains(fieldName)) {
                this.addAttributeNames(fieldName);
            }

            try {
                this.addAttributeValue(fieldName, field.get(object));
            } catch (IllegalAccessException e) {
                this.addAttributeValue(fieldName, null);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Query {");
        sb.append("Need to ").append(this.operationType);
        if (this.operationType == command.READ ||  this.operationType == command.DELETE ||   this.operationType == command.UPDATE)
            if (this.applyToAll()) sb.append(" all"); else sb.append(" one");
        sb.append(": ").append(this.name).append(",");

        if (this.operationType == command.READ ||  this.operationType == command.DELETE ||   this.operationType == command.UPDATE) {
            sb.append(" where [");
            for(int i = 0; i < this.conditions.size(); i++) {
                sb.append(conditions.get(i));
                if(i < this.conditions.size() - 1)
                    sb.append("] and [");
            }
            sb.append("]");
        }
        if (this.operationType == command.CREATE || this.operationType == command.UPDATE) {
            sb.append(" change [");
            for(int i = 0; i < this.attributeNames.size(); i++) {
                sb.append(attributeNames.get(i)).append(" = ").append(this.attributeValues.get(this.attributeNames.get(i)));
                if(i < this.attributeNames.size() - 1)
                    sb.append("] and [");
            }
            sb.append("]");
        }
        return sb.append("}").toString();
    }
}

