package NewQuery;

/**
 * Name of class: Session
 * <p>
 * Description: Used as a client for executing a transaction.
 * <p>
 * Version: 3.0
 * <p>
 * Date 04/04
 * <p>
 * Copyright: Semenov Egor, Lemain Mathieu
 */

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
