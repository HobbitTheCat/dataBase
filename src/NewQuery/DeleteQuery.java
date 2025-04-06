package NewQuery;

import NewQuery.Interfaces.Select;

import java.io.Serializable;
import java.util.Map;

/**
 * Name of class: DeleteQuery
 * <p>
 * Description: represents the delete query type, inherits from Query.
 * <p>
 * Version: 3.0
 * <p>
 * Date 03/15
 * <p>
 * Copyright: Lemain Mathieu
 */

public class DeleteQuery extends Query implements Select, Serializable {
    public DeleteQuery(String className, Map<String, String> fieldTypes) {
        super(className, command.DELETE, fieldTypes);
        for (String fieldName : fieldTypes.keySet()) {
            super.addAttributeNames(fieldName);
        }
    }

    @Override
    public DeleteQuery where(String attrName, String operator, Object value) {
        Condition condition = new Condition(attrName, operator, value);
        if (super.conditionIsApplicable(condition))
            super.addCondition(condition);
        return this;
    }

    @Override
    public DeleteQuery all() {
        super.setModifier();
        return this;
    }
}
