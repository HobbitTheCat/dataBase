package Pages;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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
    private static int version;

    public HeaderPage(ByteBuffer buffer, int pageNumber) {
        super(buffer, pageNumber);
    }

    public HeaderPage(int pageNumber, int dataBaseVersion){
        super(HeaderPage.type, (short) -1, pageNumber);
        this.setCursor((short) 0);
        this.writeBytes(HeaderPage.magicValue.getBytes()); //Text
        this.writeInteger(HeaderPage.version); //DataBaseVersion
        this.writeInteger(-1); //FirstFree
        this.writeInteger(1); //TotalPageCount
        HeaderPage.version = dataBaseVersion;
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

    public int getDataBaseVersion(){
        this.setCursor(HeaderPage.magicSize);
        return this.readInteger();
    }

    public String getMagicValue(){
        this.setCursor(0);
        return new String(this.readBytes((short) HeaderPage.magicSize), StandardCharsets.UTF_8).trim();
    }

    public String toString(){
        StringBuilder[] rows =  new StringBuilder[4];
        int maxLength = 0;
        rows[0] = new StringBuilder().append("│ MagicNumber ").append(this.getMagicValue());
        rows[1] = new StringBuilder("│ DataBaseVersion ").append(this.getDataBaseVersion());
        rows[2] = new StringBuilder().append("│ FirstFreePage ").append(this.getFirstFreePage());
        rows[3] = new StringBuilder().append("│ TotalPage ").append(this.getTotalPage());
        for(StringBuilder row: rows){
            if(row.length() > maxLength) maxLength = row.length();
        }
        return(super.assemblyString(maxLength, rows));
    }
}
