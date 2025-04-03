package NewQuery;

import Exceptions.ORMUsageException;
import NewQuery.Interfaces.Select;
import NewQuery.Interfaces.Update;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;

public class UpdateQuery extends Query implements Serializable, Update {

    public UpdateQuery(String className, Map<String, String>  fieldTypes) {
        super(className, command.UPDATE, fieldTypes);
    }

    public UpdateQuery where(String attrName, String operator, Object value) {
        Condition condition = new Condition(attrName, operator, value);
        if(super.conditionIsApplicable(condition))
            super.addCondition(condition);
        return this;
    }

    public UpdateQuery all() {
        super.setModifier();
        return this;
    }

    @Override
    public UpdateQuery attribute(String attrName, Object value) {
        if (super.hasAttribute(attrName))
            super.addAttributeNames(attrName);
        else throw new ORMUsageException("Attribute " + attrName + " is not present in " + this.getName());
        if(super.providedTypeConsistentWithClassType(attrName,  value))
            super.addAttributeValue(attrName, value);
        return this;
    }

    @Override
    public UpdateQuery object(Object object) {
        super.setAttributeObject(object);
        return this;
    }
}
