package NewQuery;

import NewQuery.Interfaces.Select;
import NewQuery.Interfaces.Update;

import java.io.Serializable;
import java.util.Map;

public class UpdateQuery extends Query implements Select, Serializable, Update {

    public UpdateQuery(String className, Map<String, String>  fieldTypes) {
        super(className, command.UPDATE, fieldTypes);
    }

    @Override
    public UpdateQuery where(String attrName, String operator, Object value) {
        Condition condition = new Condition(attrName, operator, value);
        if(super.conditionIsApplicable(condition))
            super.addCondition(condition);
        return this;
    }

    @Override
    public UpdateQuery all() {
        super.setModifier();
        return this;
    }

    @Override
    public UpdateQuery attribute(String attrName, Object value) {
        return this;
    }

    @Override
    public UpdateQuery object(Object object) {
        return this;
    }
}
