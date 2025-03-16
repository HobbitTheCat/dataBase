package Pages;

public class Address {
    private final int pageNumber;
    private final short offset;

    public int getPageNumber() {return this.pageNumber;}
    public short getOffset() {return this.offset;}

    public Address(int pageNumber, short offset) {
        this.pageNumber = pageNumber;
        this.offset = offset;
    }
    public Address(int pageNumber, int offset) {
        this.pageNumber = pageNumber;
        this.offset = (short) offset;
    }

    @Override
    public String toString() {
        return ("PageNumber: " + this.pageNumber + ", Offset: " + this.offset);
    }
}
