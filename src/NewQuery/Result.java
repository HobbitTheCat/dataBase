package NewQuery;

import java.io.Serializable;

enum statusVariants{OK, ERROR, UNKNOWN};

public class Result implements Serializable {
    // somehow stock results of one operation of transaction
    // maybe we need to stock here a list of results instead of returning a Result[]
    private statusVariants status;

    public Result(String status) {
        switch (status){
            case "OK": this.status = statusVariants.OK;
            case "ERROR": this.status = statusVariants.ERROR;
            default: this.status = statusVariants.UNKNOWN;
        }

    }

}
