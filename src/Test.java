import PageManager.MemoryManager;
import PageManager.*;
import Pages.HeaderPage;
import Pages.MetaPage;
import Pages.Page;
import TableManager.*;

import java.util.concurrent.ConcurrentHashMap;

public class Test {
    static String dataPath = "example.ehh";

    public static void main(String[] args) {


//        FilePageLoader loader = new FilePageLoader(dataPath);
//        MetaPage metaPage = new MetaPage(loader.load(1), 1);
//        System.out.println(metaPage.getClassByName("City"));
//        System.out.println(metaPage);


//        MemoryManager mm = new MemoryManager(10, dataPath);
//        PageManager pm = new PageManager(mm);
//        TableManager tm = new TableManager(pm);
//
//        String[] attrName = new String[] {"name", "capacity"};
//        String[] attrType = new String[] {"string", "string"};
//        TableDescription td = new TableDescription("City", attrName, attrType);
//        tm.createTable(td);
//
//        System.out.println(tm.searchForTable("City"));
    }

}
