package NewQuery;

import java.io.Serializable;
import java.util.Map;

public class SelectQuery extends Query implements Serializable, Select {
    public SelectQuery(String className, Map<String, String> fieldTypes) {
        super.setClassName(className);
        super.setFieldTypes(fieldTypes);
    }

    @Override
    public Select where(String attrName, String operator, Object value) {
        Condition condition = new Condition(attrName, operator, value);
        if (super.conditionIsApplicable(condition))
            super.addCondition(condition);
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
