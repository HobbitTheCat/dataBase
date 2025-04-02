package NewQuery;

import NewQuery.Interfaces.Select;

import java.io.Serializable;
import java.util.Map;

public class DeleteQuery extends Query implements Select, Serializable {
    public DeleteQuery(String className, Map<String, String> fieldTypes) {
        super(className, command.DELETE, fieldTypes);
    }

    @Override
    public DeleteQuery where(String attrName, String operator, Object value) {
        super.addCondition(new Condition(attrName, operator, value));
        return this;
    }

    @Override
    public DeleteQuery all() {
        super.setModifier();
        return this;
    }
}
