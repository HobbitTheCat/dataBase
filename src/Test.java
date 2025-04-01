import NewQuery.Condition;
import PageManager.MemoryManager;
import PageManager.*;
import Pages.*;
import TableManager.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

        LongPage page = new LongPage(loader.load(3), 3);
        System.out.println(page);

    }
    public static void createDB(){
        MemoryManager mm = new MemoryManager(10, dataPath);
        PageManager pm = new PageManager(mm);
        TableManager tm = new TableManager(pm);

        String[] attrName = new String[] {"capacity", "name"};
        Map<String, String> attrTypes = new HashMap<>();
        attrTypes.put("capacity", "Integer");
        attrTypes.put("name", "string");
        TableDescription td = new TableDescription("City", attrName, attrTypes);

        tm.createTableIfNotExist(td);
//        Map<String, Object> attrValues = new  HashMap<>();
//        attrValues.put("capacity", 1050);
//        attrValues.put("name", "Paris");
//        tm.addObject(td, attrValues);

        Condition cond1 = new Condition("name", "==", "paris");
        Condition  cond2 = new Condition("capacity", "==", 1050);
        ArrayList<Condition> conditionList = new ArrayList<>();
        conditionList.add(cond1);
        conditionList.add(cond2);

        ArrayList<Map<String, Object>> map = tm.searchObject(td, conditionList);
        Map<String, Object>[] canByStringify = map.toArray(new Map[0]);
        System.out.println(Arrays.toString(canByStringify));


        System.out.println(tm.searchForTable("City"));
    }
}
