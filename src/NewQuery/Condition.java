package NewQuery;

import java.io.Serializable;

/**
 * @param attributeName target set name
 * @param operator         ==, !=, >, <, >=, <=
 * @param value target value
 * if the condition is imposed on an set that does not exist, it is ignored.
 * absence of conditions leads to giving away all existing objects
 */
public record Condition(String attributeName, String operator, Object value) implements Serializable {
    @Override
    public String toString() {
        return attributeName + " " + operator + " " + value;
    }
}
