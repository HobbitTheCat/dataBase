package Pages;

import java.nio.ByteBuffer;

public class HeaderPage extends Page{
    /**
     * Header page construction:
     *  - MetaInfo of Page 10 bytes ?
     *  - MagicNumber HeaderPage.magicSize
     *  - DataBaseVersion int 4 bytes
     *  - FirstFreeIndex int 4 bytes
     *  - Total page count 4 bytes
     */
    private static final short type = 100;
    private static final String magicValue = "PROJECTDATA";
    private static final int magicSize = magicValue.getBytes().length;
    private static final int version = 1;

    public HeaderPage(ByteBuffer buffer, int pageNumber) {
        super(buffer, pageNumber);
    }

    public HeaderPage(int pageNumber){
        super(HeaderPage.type, (short) -1, pageNumber);
        this.setCursor((short) 0);
        this.writeBytes(HeaderPage.magicValue.getBytes()); //Text
        this.writeInteger(HeaderPage.version); //DataBaseVersion
        this.writeInteger(-1); //FirstFree
        this.writeInteger(1); //TotalPageCount
    }

    public void setFirstFree(int pageNumber){
        this.setCursor(HeaderPage.magicSize + 4);
        this.writeInteger(pageNumber);
    }

    public int getFirstFreePage(){
        this.setCursor(HeaderPage.magicSize + 4);
        return this.readInteger();
    }

    public void setTotalPage(int totalPageNumber){
        this.setCursor(HeaderPage.magicSize + 8);
        this.writeInteger(totalPageNumber);
    }

    public int getTotalPage(){
        this.setCursor(HeaderPage.magicSize + 8);
        return this.readInteger();
    }

}
