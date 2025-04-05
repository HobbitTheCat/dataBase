package Pages;

import java.nio.ByteBuffer;

/**
 * Name of class: FreePage
 * <p>
 * Description: Represents a blank page that replaces a deallocated page.
 * <p>
 * Version: 3.0
 * <p>
 * Date 03/25
 * <p>
 * Copyright: Lemain Mathieu
 */

public class FreePage extends Page {
    /**
     * Free page construction:
     *  - Page Meta info 10 bytes ?
     */
    private static final short type = 99;

    public FreePage(int pageNumber){
        super(FreePage.type, (short) -1, pageNumber);
        setNextFreePage(-1);
    }
    public FreePage(ByteBuffer buffer, int pageNumber){super(buffer, pageNumber);}

    public void setNextFreePage(int pageNumber){
        this.setNextPage(pageNumber);
    }
    public int getNextFreePage(){
        this.getNextPage();
        return this.readInteger();
    }

    public String toString(){
        StringBuilder[] rows = new  StringBuilder[0];
        return super.assemblyString(15, rows);
    }
}
