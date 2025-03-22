import Pages.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TableManager {
    private final int pageSize = 4096;
    private final VirtualMemoryManager vMM;

    public TableManager(VirtualMemoryManager vMM) {
        this.vMM = vMM;
    }

    private Page readPage(int pageIndex) {
//        ByteBuffer pageData = this.vMM.loadPage(pageIndex);
//        return PageFactory.createPage(pageData, pageIndex);
        return new HeaderPage(0);
    }

    private DataClass searchClass(String className){
        MetaPage metaPage = (MetaPage) this.readPage(0);
        DataClass data;
        do{
            data = metaPage.getClassByName(className);
            if (data != null) return data;
            metaPage = (MetaPage) this.readPage(metaPage.getNextPage());
        }while(metaPage.getNextPage() != -1);
        return null;
    }


    public ArrayList<Map<String, Object>> loadData(String className) {
        // └> list of object └> name  └> value
        DataClass classInfo = this.searchClass(className);
        if(classInfo == null) return null;

        ArrayList<Map<String, Object>> result = new ArrayList<>();
        int objectPageIndex = classInfo.getObjectPage();

        while(objectPageIndex != -1){
            ObjectPage objectPage = (ObjectPage) this.readPage(objectPageIndex);
            for(short i = 0; i < objectPage.size(); i++){
                Map<String, Object> data = new HashMap<>();
                Address[] attributeAddresses = objectPage.get(i);
                for(int j = 0; j < attributeAddresses.length; j++){
                    String attrName = classInfo.getAttributesNames()[j];
                    int attrPageIndex = attributeAddresses[j].getPageNumber();
                    Page attrPage = this.readPage(attrPageIndex);
                    Object value = null;
                    if(attrPage instanceof StringPage sp) value = sp.get(attributeAddresses[j].getOffset()); // так тут явно не offset а index, // так что нужно переписать класс DataClass и все места гже он создается
                    data.put(attrName, value);
                }
                result.add(data);
            }
            objectPageIndex = objectPage.getNextPage();
        }
        return result;
    }

}
