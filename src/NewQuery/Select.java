package NewQuery;

public interface Select{
    Select where(String attrName, String operator, String value);
    Select first();
    Select all();
}
