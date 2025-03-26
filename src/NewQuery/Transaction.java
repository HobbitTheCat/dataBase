package NewQuery;

public class Transaction {
    Query[] queries;

    Transaction() {

    }

    public <T extends Select> void add(T selectQuery) {
        // Logique d'addition select-query
    }

    public <T extends Update> void add(T updateQuery) {
        // Logique d'addition update-query
    }

    public <T extends CreateQuery> void add(T createQuery) {
        // Logique d'addition create-query
    }

    public <T extends DeleteQuery> void add(T deleteQuery) {
        // Logique d'addition delete-query
    }
    public void remove(int index){
        // remove query at index
    }

}
