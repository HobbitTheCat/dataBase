package NewQuery;

public interface Select{
    Select where(String attrName, String operator, Object value);
    Select first();
    Select all();
}
