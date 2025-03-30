package NewQuery;

/**
 * @param attributeName target attribute name
 * @param operator         ==, !=, >, <, >=, <=
 * @param value target value
 * if the condition is imposed on an attribute that does not exist, it is ignored.
 * absence of conditions leads to giving away all existing objects
 */
public record Condition(String attributeName, String operator, Object value) {
    @Override
    public String toString() {
        return attributeName + " " + operator + " " + value;
    }
}
