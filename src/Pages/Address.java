package Pages;

/**
 * Name of class: Address
 * <p>
 * Description: Represents the adress of a piece of data using the page index and an offset.
 * <p>
 * Version: 4.0
 * <p>
 * Date 03/25
 * <p>
 * Copyright: Lemain Mathieu
 */

public class Address {
    public static final int ADDRESS_SIZE = 6;
    private final int pageNumber;
    private final short offset;

    public int getPageNumber() {return this.pageNumber;}
    public short getOffset() {return this.offset;}
    public boolean isNull(){
        return this.pageNumber == -1 && this.offset == -1;
    }

    public Address(int pageNumber, short offset) {
        this.pageNumber = pageNumber;
        this.offset = offset;
    }
    public Address(int pageNumber, int offset) {
        this.pageNumber = pageNumber;
        this.offset = (short) offset;
    }


    public boolean equals(Address address){
        return this.pageNumber == address.getPageNumber() && this.offset == address.getOffset();
    }

    @Override
    public String toString() {
        return ("(PageNumber: " + this.pageNumber + ", Offset: " + this.offset + ")");
    }
}
