package NewQuery;

public class Session {

    public Session() {
        
    }

    public Transaction createNewTransaction(){
        int transactionNumber = 0; // here I need to send request to obtain unique transaction number
        return new Transaction(transactionNumber);
    }

    public Result[] execute(Transaction transaction){
        // execute transaction
        return null;
    }
}
