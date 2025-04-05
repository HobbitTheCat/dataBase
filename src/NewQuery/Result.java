package NewQuery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

enum statusVariants{OK, ERROR, UNKNOWN};

/**
 * Name of class: Result
 * <p>
 * Description: Data type for storing the result of a Query once it's been executed.
 * <p>
 * Version: 2.0
 * <p>
 * Date 03/17
 * <p>
 * Copyright: Semenov Egor
 */

public class Result implements Serializable {
    // somehow stock results of one operation of transaction
    // maybe we need to stock here a list of results instead of returning a Result[]
    private ArrayList<Map<String, Object>> objectsData;
    private final statusVariants status;
    private String errorMessage;

    public String getErrorMessage() {return errorMessage;}
    public String getStatus() {return status.toString();}
    public ArrayList<Map<String, Object>> getObjectData() {return  objectsData;}

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

    public Result(String status, ArrayList<Map<String, Object>> objectsData){
        switch (status){
            case "OK" -> {
                this.status = statusVariants.OK;
                this.objectsData = objectsData;
            }
            case "ERROR" -> {
                this.status = statusVariants.ERROR;
            }default -> this.status = statusVariants.UNKNOWN;
        }
    }

}
