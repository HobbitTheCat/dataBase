package QueryGeneration;

import NewQuery.Condition;

import java.util.List;

/**
 * @param action  read, create, etc.
 * @param resourceName class name
 * @param conditions condition list
 */
public record Query(String action, String resourceName, List<Condition> conditions){
    @Override
    public String toString() {
        return "Action: " + action + ", Resource: " + resourceName + ", Conditions: " + conditions;
    }
}
