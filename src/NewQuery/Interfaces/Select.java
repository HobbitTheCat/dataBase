package NewQuery.Interfaces;

public interface Select{
    Select where(String attrName, String operator, Object value);
    Select all();
}
