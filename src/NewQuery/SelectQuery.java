package NewQuery;

import java.io.Serializable;

public class SelectQuery extends Query implements Serializable, Select {

    @Override
    public Select where(String attrName, String operator, Object value) {
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
