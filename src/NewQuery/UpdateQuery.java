package NewQuery;

import java.io.Serializable;

public class UpdateQuery extends Query implements Select, Serializable, Update {
    @Override
    public Select where(String attrName, String operator, String value) {
        return null;
    }

    @Override
    public Select first() {
        return null;
    }

    @Override
    public Select all() {
        return null;
    }

    @Override
    public Update column(String... columnNames) {
        return null;
    }

    @Override
    public Update values(String... values) {
        return null;
    }

    @Override
    public Update object(Object object) {
        return null;
    }
}
