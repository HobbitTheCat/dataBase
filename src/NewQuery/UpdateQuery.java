package NewQuery;

import java.io.Serializable;

public class UpdateQuery extends Query implements Select, Serializable, Update {
    @Override
    public Select where(String attrName, String operator, String value) {
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
    public Update values(String... values) {
        return this;
    }

    @Override
    public Update object(Object object) {
        return this;
    }
}
