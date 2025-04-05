package NewQuery;

import Exceptions.ORMUsageException;
import NewQuery.Interfaces.Update;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Name of class: CreateQuery
 * <p>
 * Description: Represents the create query type, inherits from Query
 * <p>
 * Version: 3.0
 * <p>
 * Date 03/15
 * <p>
 * Copyright: Lemain Mathieu
 */

public class CreateQuery extends Query implements Update, Serializable {
    public CreateQuery(String className, Map<String, String>  fieldTypes) {
        super(className, command.CREATE, fieldTypes);
        for (String fieldName : fieldTypes.keySet()) {
            super.addAttributeNames(fieldName);
        }
    }

    @Override
    public Update set(String attrName, Object value) {
        if (!super.hasAttribute(attrName))
            throw new ORMUsageException("Attribute " + attrName + " is not present in " + this.getName());

        if (super.providedTypeConsistentWithClassType(attrName, value))
            super.addAttributeValue(attrName, value);

        return this;
    }

    @Override
    public Update object(Object object) {
        super.setAttributeObject(object);
        return this;
    }
}
