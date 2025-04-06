package NewQuery;
import NewQuery.*;

import java.util.ArrayList;
import java.util.Objects;
import java.util.StringTokenizer;

public class NewInterpreter {
    public ArrayList<Query> readScript(String script, boolean verbose) {


        StringTokenizer tokenizer = new StringTokenizer(script, "\n\t\f ");
        String token;
        int state = 0;
        boolean valid = false;

        Transaction currentTransaction = new Transaction();
        Query currentQuery = null;
        String currentQueryType = null;
        String currentOperator = null;
        String currentClassName = null;
        String currentObjectName = null;
        String currentValue = null;

        label:
        while (tokenizer.hasMoreTokens()) {
            token = tokenizer.nextToken();
            if (state == 0) {
                if(token.equals("start")){
                    state = 1;
                }
                else{
                    break label;
                }
            }
            else if (state == 1) {
                if(token.equals("end")){
                    valid = true;
                    break label;
                }
                else if(token.equals("select")){
                    currentQueryType = token;
                    state = 12;
                }
                else if(token.equals("create")){
                    currentQueryType = token;
                    state = 4;
                }
                else if(token.equals("update")){
                    currentQueryType = token;
                    state = 3;
                }
                else if(token.equals("delete")){
                    currentQueryType = token;
                    state = 2;
                }
                else{
                    break label;
                }
            }
            else if (state == 2) {
                //token should be a class name
                if(isName(token)) {
                    state = 18;
                }
                else{
                    break label;
                }
            }
            else if (state == 3) {
                if(isName(token)) {
                    state = 11;
                }
                else{
                    break label;
                }
            }
            else if (state == 4) {
                if(isName(token)) {
                    state = 10;
                }
                else{
                    break label;
                }
            }
            else if (state == 5) {
                if(token.equals("where")){
                    state = 6;
                }
                else if(token.equals("all")){
                    state = 1;
                }
                else if(token.equals("limit")){
                    state = 9;
                }
                else{
                    break label;
                }
            }
            else if (state == 6) {
                if(isName(token)) {
                    state = 7;
                }
                else{
                    break label;
                }
            }
            else if (state == 7) {
                if(isComparisonOperator(token)) {
                    state = 8;
                }
                else{
                    break;
                }
            }
            else if (state == 8) {
                state = 5;
            }
            else if (state == 9) {
                if(token.matches("^[1-9][0-9]*$")) {//token must be a positive integer
                    state = 1;
                }
                else{
                    break label;
                }
            }
            else if(state == 10){
                if(isName(token)) {
                    state = 1;
                }
                else{
                    break label;
                }
            }
            else if (state == 11) {
                if(token.equals("where")){
                    state = 13;
                }
                else if(token.equals("set")){
                    state = 16;
                }
                else{
                    break label;
                }
            }
            else if (state == 12) {
                if(isName(token)) {
                    state = 5;
                }
                else{
                    break label;
                }
            }
            else if (state == 13) {
                if(isName(token)) {
                    state = 14;
                }
                else{
                    break label;
                }
            }
            else if (state == 14) {
                if(isComparisonOperator(token)) {
                    state = 15;
                }
                else{
                    break label;
                }
            }
            else if (state == 15) {
                state = 11;
            }
            else if (state == 16) {
                if(isName(token)) {
                    state = 17;
                }
                else{
                    break label;
                }
            }
            else if (state == 17) {
                state = 1;
            }
            else if (state == 18) {
                if(token.equals("where")){
                    state = 19;
                }
                else if(token.equals("all")){
                    state = 1;
                }
            }
            else if (state == 19) {
                if(isName(token)) {
                    state = 20;
                }
                else{
                    break label;
                }
            }
            else if (state == 20) {
                if(isComparisonOperator(token)) {
                    state = 21;
                }
                else{
                    break label;
                }
            }
            else if (state == 21) {
                state = 18;
            }
            else{
                break label;
            }
        }
        if(!valid) {
            System.out.println("Syntax error");
        }else{
            System.out.println("Compiled with success");
        }
    }

    public static boolean isName(String word){
        return isNotKeyword(word) && word.matches("^[a-zA-Z]+$");
    }
    public static boolean isNotKeyword(String word) {
        return !word.equals("start")
                && !word.equals("create")
                && !word.equals("select")
                && !word.equals("update")
                && !word.equals("delete")
                && !word.equals("from")
                && !word.equals("where")
                && !word.equals("contains")
                && !word.equals("all")
                && !word.equals("end")
                && !word.equals("limit")
                && !word.equals("set");
    }
    public static boolean isComparisonOperator(String word) {
        return word.equals("==")
                || word.equals("!=")
                || word.equals(">")
                || word.equals("<")
                || word.equals(">=")
                || word.equals("<=")
                || word.equals("contains");
    }
    public static boolean isQueryType(String queryType) {
        return queryType.equals("create")
                || queryType.equals("select")
                || queryType.equals("update")
                || queryType.equals("delete");
    }
}