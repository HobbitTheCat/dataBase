package QueryGeneration;

import NewQuery.Condition;

import java.util.*;

public class Interpreter {
    public ArrayList<Query> readScript(String script, boolean verbose) {
        ArrayList<Query> queries = new ArrayList<>();

        List<Condition> conditions = new ArrayList<>();

        StringTokenizer tokenizer = new StringTokenizer(script, "\n\t\f ");
        String token;
        int state = 0;
        boolean valid = false;
        String currentQueryType = null;
        String currentClassName = null;
        String currentAttributeName = null;
        String currentAttributeValue = null;
        String currentComparisonOperator = null;
        String currentLogicOperator;
        label:
        while (tokenizer.hasMoreTokens()) {
            token = tokenizer.nextToken();
            if(state == 0) {
                if(Objects.equals(token, "start")){
                    queries.add(new Query("start", null, null));
                    state = 1;
                }
                else{
                    break;
                }
            }
            else if(state == 1) {
                if(isQueryType(token)) {
                    currentQueryType = token;
                    state = 2;
                }
                else{
                    break;
                }
            }
            else if(state == 2){
                if(Objects.equals(token, "from")){
                    state = 3;
                }
                else{
                    break;
                }
            }
            else if(state == 3){
                if(isNotKeyword(token)){
                    currentClassName = token;
                    state = 4;
                }
                else{
                    break;
                }
            }
            else if(state == 4){
                if(Objects.equals(token, "where")){
                    state = 5;
                }
                else{
                    break;
                }
            }
            else if(state == 5){
                if(isNotKeyword(token)){
                    currentAttributeName = token;
                    state = 6;
                }
                else{
                    break;
                }
            }
            else if(state == 6){
                if(isComparisonOperator(token)){
                    currentComparisonOperator = token;
                    state = 7;
                }
                else{
                    break;
                }
            }
            else if(state == 7){
                currentAttributeValue = token;
                state = 8;
            }
            else if(state == 8){
                currentLogicOperator = null;
                switch (token) {
                    case "or":
                    case "and":
                        currentLogicOperator = token;
                        state = 5;
                        break;
                    case "read":
                    case "create":
                    case "update":
                    case "delete":
                        queries.add(new Query(currentQueryType, currentClassName, conditions));
                        state = 2;
                        break;
                    case "end":
                        if (!tokenizer.hasMoreTokens()) {
                            valid = true;
                        }
                        state = 9;
                        break;
                    default:
                        break label;
                }
                conditions.add(
                        new Condition(
                                currentAttributeName,
                                currentComparisonOperator,
                                currentAttributeValue,
                                currentLogicOperator
                        )
                );
            }
            else {
                queries.add(new Query(currentQueryType, currentClassName, conditions));
                queries.add(new Query("end", null, null));
                if(Objects.equals(token, "start")){
                    queries.add(new Query("start", null, null));
                    state = 1;
                }
                else{
                    break;
                }
            }
        }
        if(!valid) {
            System.out.println("Syntax error");
        }else{
            System.out.println("Compiled with success");
        }
        if(verbose) {
            System.out.println(queries);
        }
        return queries;
    }
    public static boolean isNotKeyword(String word) {
        return !word.equals("start")
                && !word.equals("create")
                && !word.equals("read")
                && !word.equals("update")
                && !word.equals("delete")
                && !word.equals("from")
                && !word.equals("where")
                && !word.equals("and")
                && !word.equals("or")
                && !word.equals("not")
                && !word.equals("end");
    }
    public static boolean isComparisonOperator(String word) {
        return word.equals("==")
                || word.equals("!=")
                || word.equals(">")
                || word.equals("<")
                || word.equals(">=")
                || word.equals("<=");
    }
    public static boolean isQueryType(String queryType) {
        return queryType.equals("create")
                || queryType.equals("read")
                || queryType.equals("update")
                || queryType.equals("delete");
    }
}