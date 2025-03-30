import PageManager.MemoryManager;
import PageManager.*;
import Pages.*;
import TableManager.*;

import java.util.concurrent.ConcurrentHashMap;

public class Test {
    static String dataPath = "example.ehh";

    public static void main(String[] args) {
        createDB();

        FilePageLoader loader = new FilePageLoader(dataPath);
        MetaPage metaPage = new MetaPage(loader.load(1), 1);
        System.out.println(metaPage);

        ObjectPage objectPage = new ObjectPage(loader.load(2), (short)12, 2);
        System.out.println(objectPage);

        StringPage page = new StringPage(loader.load(3), 3);
        System.out.println(page);

    }
    public static void createDB(){
        MemoryManager mm = new MemoryManager(10, dataPath);
        PageManager pm = new PageManager(mm);
        TableManager tm = new TableManager(pm);

        String[] attrName = new String[] {"capacity", "name"};
        String[] attrType = new String[] {"string", "string"};
        TableDescription td = new TableDescription("City", attrName, attrType);

//        tm.createTable(td);
        tm.addObject(td, new String[]{"150", "Paris"});

        System.out.println(tm.searchForTable("City"));
    }
}
