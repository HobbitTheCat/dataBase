package NewQuery;

import java.io.Serializable;
import java.util.Map;

public class UpdateQuery extends Query implements Select, Serializable, Update {

    public UpdateQuery(String className, Map<String, String>  fieldTypes) {
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

    @Override
    public Update column(String... columnNames) {

        return this;
    }

    @Override
    public Update values(Object... values) {
        return this;
    }

    @Override
    public Update object(Object object) {
        return this;
    }
}
