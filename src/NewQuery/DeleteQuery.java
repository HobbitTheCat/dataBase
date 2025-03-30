package NewQuery;

import java.io.Serializable;
import java.util.Map;

public class DeleteQuery extends Query implements Select, Serializable {
    public DeleteQuery(String className, Map<String, String> fieldTypes) {
        super.setClassName(className);
        super.setFieldTypes(fieldTypes);
    }

    @Override
    public Select where(String attrName, String operator, Object value) {
        super.addCondition(new Condition(attrName, operator, value));
        return this;
    }

    @Override
    public Select first() {
        return this;
    }

    @Override
    public Select all() {
        return this;
    }
}
