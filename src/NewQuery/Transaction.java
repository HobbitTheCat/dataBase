package NewQuery;

import NewQuery.Interfaces.Select;
import NewQuery.Interfaces.Update;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Name of class: Transaction
 * <p>
 * Description: A set of queries that supports ACID properties
 * <p>
 * Version: 4.0
 * <p>
 * Date 03/30
 * <p>
 * Copyright: Lemain Mathieu
 */

public class Transaction implements Serializable {
    private final ArrayList<Query> queries;
    private int transactionId = -1;

    public int getTransactionId() {return transactionId;}
    public  Query[] getQueries() {return queries.toArray(new Query[0]);}

    public Transaction() {
        this.queries = new ArrayList<>();
    }

    public void changeTransactionId(int newTransactionId){
        this.transactionId = newTransactionId;
    }

    public void add(Select selectQuery) { this.queries.add((Query) selectQuery); }
    public void add(Update updateQuery) { this.queries.add((Query) updateQuery); }


    public void remove(int index){
        this.queries.remove(index);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Transaction ").append(transactionId).append("{\n");
        for (Query query : this.getQueries())
            sb.append(query.toString()).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
