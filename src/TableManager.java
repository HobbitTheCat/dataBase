import Pages.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

public class TableManager {
    private final int pageSize = 4096;
    private final VirtualMemoryManager vMM;

    public TableManager(VirtualMemoryManager vMM) {
        this.vMM = vMM;
    }

    private Page readPage(int pageIndex, int type) {
        ByteBuffer pageData = this.vMM.loadPage(pageIndex);
        return switch (pageData.getShort(0)) {
            case 0 -> new MetaPage(pageData, pageIndex);
            case 1 -> new ObjectPage(pageData, pageData.getShort(8), pageIndex);
            case 2 -> new StringPage(pageData, pageIndex);
            default -> null;
        };
    }

    private DataClass searchClass(String className){
        MetaPage metaPage = (MetaPage) this.readPage(0, 0);
        DataClass data;
        do{
            data = metaPage.getClassByName(className);
            if (data != null) return data;
            metaPage = (MetaPage) this.readPage(metaPage.getNextPage(), 0);
        }while(metaPage != null && metaPage.getNextPage() != -1);
        return null;
    }


    public ArrayList<Map<String, Object>> loadData(String className) {
        // └> list of object └> name  └> value
        DataClass classInfo = this.searchClass(className);
        if(classInfo == null) return null;
        for(int i = 0; i < classInfo.getAttributesNames().length; i++){
            Page page;
            do {
                page = this.readPage(classInfo.getAttributePageByName(classInfo.getAttributesNames()[i]), 1);
                if (page == null) return null;
                if (page.getType() == 2){
                    for (int j = 0; j < page.getOnPageObjectNumber(); j++) {

                    }
                }
            }while (page.getNextPage() != -1);
        }
    }

}
