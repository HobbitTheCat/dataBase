package Query;

/**
 * @param operator         ==, !=, >, <, >=, <=
 * @param logicalConnector and, or, null, null if it is the last state
 */
public record Condition(String attributeName, String operator, String value, String logicalConnector) {
    @Override
    public String toString() {
        return attributeName + " " + operator + " " + value + (logicalConnector != null ? " " + logicalConnector : "");
    }
}
