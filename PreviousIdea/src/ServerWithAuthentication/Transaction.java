package ServerWithAuthentication;

import NewQuery.Interfaces.Select;
import NewQuery.Interfaces.Update;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Name of class: Transaction
 * <p>
 * Description: A set of queries that supports ACID properties
 * <p>
 * Version: 4.1
 * <p>
 * Date 04/06
 * <p>
 * Copyright: Lemain Mathieu
 */

public class Transaction implements Serializable {
    private final ArrayList<Query> queries;
    private int transactionId = -1;
    private int clientId = -1; // ID of the client to which the transaction belongs

    public int getTransactionId() {return transactionId;}
    public int getClientId() {return clientId;}
    public  Query[] getQueries() {return queries.toArray(new Query[0]);}

    public Transaction() {
        this.queries = new ArrayList<>();
    }

    public void changeTransactionId(int newTransactionId){
        this.transactionId = newTransactionId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public void add(Select selectQuery) { this.queries.add((Query) selectQuery); }
    public void add(Update updateQuery) { this.queries.add((Query) updateQuery); }


    public void remove(int index){
        this.queries.remove(index);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Transaction ").append(transactionId);
        if (clientId != -1) {
            sb.append(" (Client ").append(clientId).append(")");
        }
        sb.append("{\n");
        for (Query query : this.getQueries())
            sb.append(query.toString()).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
