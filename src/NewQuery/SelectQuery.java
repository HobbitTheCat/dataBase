package NewQuery;

import NewQuery.Interfaces.Select;

import java.io.Serializable;
import java.util.Map;

public class SelectQuery extends Query implements Serializable, Select {
    public SelectQuery(String className, Map<String, String> fieldTypes) {
        super(className, command.READ, fieldTypes);
    }

    @Override
    public Select where(String attrName, String operator, Object value) {
        Condition condition = new Condition(attrName, operator, value);
        if (super.conditionIsApplicable(condition))
            super.addCondition(condition);
        return this;
    }
    
    @Override
    public Select all() {
        super.setModifier();
        return this;
    }
}
