package NewQuery;

import NewQuery.Interfaces.Select;
import NewQuery.Interfaces.Update;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class Transaction implements Serializable {
    private final ArrayList<Query> queries;
    private final int transactionId;

    public int getTransactionId() {return transactionId;}
    public  Query[] getQueries() {return queries.toArray(new Query[0]);}

    Transaction(int transactionId) {
        this.transactionId = transactionId;
        this.queries = new ArrayList<>();
    }

    public <T extends Select> void add(T selectQuery) {this.queries.add((Query) selectQuery);}
    public <T extends Update> void add(T updateQuery) {this.queries.add((Query) updateQuery);}
    public <T extends CreateQuery> void add(T createQuery) {this.queries.add((Query) createQuery);}
    public <T extends DeleteQuery> void add(T deleteQuery) {this.queries.add((Query) deleteQuery);}


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
