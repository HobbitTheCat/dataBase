package TableManager.Exceptions;

public class TableAlreadyExistException extends TableManagementException {
    public TableAlreadyExistException(String message) {
        super(message);
    }
}
