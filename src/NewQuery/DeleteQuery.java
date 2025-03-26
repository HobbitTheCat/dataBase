package NewQuery;

import java.io.Serializable;

public class DeleteQuery extends Query implements Select, Serializable {
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
}
