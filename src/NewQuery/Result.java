package NewQuery;

import java.io.Serializable;

enum statusVariants{OK, ERROR, UNKNOWN};

public class Result implements Serializable {
    // somehow stock results of one operation of transaction
    // maybe we need to stock here a list of results instead of returning a Result[]
    private statusVariants status;
    private String errorMessage;

    public String getErrorMessage() {return errorMessage;}
    public String getStatus() {return status.toString();}

    public Result(String status) {
        switch (status){
            case "OK": this.status = statusVariants.OK; break;
            case "ERROR": this.status = statusVariants.ERROR;  break;
            default: this.status = statusVariants.UNKNOWN;
        }
    }

    public Result(String status, String errorMessage) {
        switch (status){
            case "OK": this.status = statusVariants.OK;  break;
            case "ERROR": this.status = statusVariants.ERROR; break;
            default: this.status = statusVariants.UNKNOWN;
        } this.errorMessage = errorMessage;
    }

}
