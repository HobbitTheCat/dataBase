package NewQuery;

/**
 * @param operator         ==, !=, >, <, >=, <=
 */
public record Condition(String attributeName, String operator, String value) {
    @Override
    public String toString() {
        return attributeName + " " + operator + " " + value;
    }
}
