package Pages;

import java.nio.ByteBuffer;

/**
 * Name of class: PageFactory
 * <p>
 * Description: Generates a page from a page content.
 * <p>
 * Version: 1.0
 * <p>
 * Date 03/30
 * <p>
 * Copyright: Lemain Mathieu
 */

public class PageFactory {
    public static Page createPage(ByteBuffer buffer, int pageIndex){
        int type = buffer.getShort(0);
        return switch (type){
            case 0 -> {yield new MetaPage(buffer, pageIndex);}
            case 1 -> {yield new ObjectPage(buffer, buffer.getShort(8), pageIndex);}
            case 2 -> {yield new StringPage(buffer, pageIndex);}
            case 3 -> {yield new LongPage(buffer, pageIndex);}
            case 99 -> {yield  new FreePage(buffer, pageIndex);}
            case 100 -> {yield  new HeaderPage(buffer, pageIndex);}
            default -> throw new IllegalArgumentException("Unknown PageType: " + type);
        };
    }
}
