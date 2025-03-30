package NewQuery;

import java.io.Serializable;
import java.util.ArrayList;

public class Transaction implements Serializable {
    private ArrayList<Query> queries;
    private final int transactionId;

    public int getTransactionId() {return transactionId;}
    public  Query[] getQueries() {return queries.toArray(new Query[0]);}

    Transaction(int transactionId) {
        this.transactionId = transactionId;
    }

    public <T extends Select> void add(T selectQuery) {this.queries.add((Query) selectQuery);}
    public <T extends Update> void add(T updateQuery) {this.queries.add((Query) updateQuery);}
    public <T extends CreateQuery> void add(T createQuery) {this.queries.add((Query) createQuery);}
    public <T extends DeleteQuery> void add(T deleteQuery) {this.queries.add((Query) deleteQuery);}


    public void remove(int index){
        this.queries.remove(index);
    }

}
