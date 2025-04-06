package NewQuery;

import NewQuery.Interfaces.Select;

import java.io.Serializable;
import java.util.Map;

/**
 * Name of class: SelectQuery
 * <p>
 * Description: Represents the SelectQuery
 * <p>
 * Version: 3.0
 * <p>
 * Date 03/17
 * <p>
 * Copyright: Lemain Mathieu
 */

public class SelectQuery extends Query implements Serializable, Select {
    public SelectQuery(String className, Map<String, String> fieldTypes) {
        super(className, command.READ, fieldTypes);
        for (String fieldName : fieldTypes.keySet()) {
            super.addAttributeNames(fieldName);
        }
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
