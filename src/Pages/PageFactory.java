package Pages;

import java.nio.ByteBuffer;

public class PageFactory {
    public static Page createPage(ByteBuffer buffer, int pageIndex){
        int type = buffer.getShort(0);
        return switch (type){
            case 0 -> {yield new MetaPage(buffer, pageIndex);}
            case 1 -> {yield new ObjectPage(buffer, buffer.getShort(8), pageIndex);}
            case 2 -> {yield new StringPage(buffer, pageIndex);}
            default -> throw new IllegalArgumentException("Unknown PageType: " + type);
        };
    }
}
