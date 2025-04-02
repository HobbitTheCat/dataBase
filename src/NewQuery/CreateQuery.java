package NewQuery;

import Exceptions.ORMUsageException;
import NewQuery.Interfaces.Update;

import java.util.Map;

public class CreateQuery extends Query implements Update {
    public CreateQuery(String className, Map<String, String>  fieldTypes) {
        super(className, command.CREATE, fieldTypes);
    }

    @Override
    public Update attribute(String attrName, Object value) {
        if (super.hasAttribute(attrName))
            super.addAttributeNames(attrName);
        else throw new ORMUsageException("Attribute " + attrName + " is not present in " + this.getName());
        if(super.providedTypeConsistentWithClassType(attrName,  value))
            super.addAttributeValue(attrName, value);
        return this;
    }

    @Override
    public Update object(Object object) {
        return this;
    }
}
