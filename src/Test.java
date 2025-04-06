import NewQuery.Condition;
import PageManager.MemoryManager;
import PageManager.*;
import Pages.*;
import TableManager.*;

import java.nio.ByteBuffer;
import java.util.*;

public class Test {
//    static String dataPath = "finalVersion.ehh";
    static String dataPath = "example.ehh"; //before testing you need to delete example.ehh You may not delete it, but it will create a conflict with already created objects. Unpredictable behavior
    static MemoryManager mm = new MemoryManager(10, dataPath);
    static PageManager pm = new PageManager(mm);
    static TableManager tm = new TableManager(pm);

    public static void main(String[] args) {
        TableDescription td = createTable();
        createObjects(td);
        deleteObjects(td);
//        searchObjects(td);

        FilePageLoader loader = new FilePageLoader(dataPath);
        MetaPage metaPage = new MetaPage(loader.load(1), 1);
        System.out.println(metaPage);

        HeaderPage hp = new HeaderPage(loader.load(0), 0);
        System.out.println(hp);

//        TableDescription td = new TableDescription("Table2", new String[]{}, new HashMap<>());
//        tm.deleteTable(td);


//
        ByteBuffer buffer = loader.load(2);
        ObjectPage objectPage = new ObjectPage(buffer, buffer.getShort(8), 2);
        System.out.println(objectPage);


//        LongPage page = new LongPage(loader.load(3), 3);
//        StringPage page = new StringPage(loader.load(4),4);
//        System.out.println(page);

    }

    public static TableDescription createTable(){
        String[] attrName = new String[] {"name", "capacity"};
        Map<String, String> attrTypes = new HashMap<>();
        attrTypes.put("name", "String");
        attrTypes.put("capacity", "Integer");
        TableDescription td = new TableDescription("City", attrName, attrTypes);
        tm.createTableIfNotExist(td);
        return td;
    }

    public static void createTablesTest() {
        List<TableDescription> tables = new ArrayList<>();

        for (int i = 1; i <= 11; i++) {
            String tableName = "Table" + i;
            String[] attrNames = new String[10];
            Map<String, String> attrTypes = new HashMap<>();

            for (int j = 1; j <= 10; j++) {
                String attrName = "attr" + j;
                attrNames[j - 1] = attrName;

                String type;
                switch (j % 2) {
                    case 1:
                        type = "String";
                        break;
                    default:
                        type = "Integer";
                        break;
                }

                attrTypes.put(attrName, type);
            }

            TableDescription td = new TableDescription(tableName, attrNames, attrTypes);
            tm.createTableIfNotExist(td);
            tables.add(td);
        }
    }

    public static void createObjects(TableDescription td){
        String[] cityNames = new String[]{"Dijon", "Paris", "Seoul", "London", "Daegu", "Saint-Petersburg", "Moscow"};

        for(int i = 0; i < 100; i ++) {
            Random random = new Random();
            Map<String, Object> attrValues = new HashMap<>();
            attrValues.put("capacity", random.nextInt(1000, 100000));
            attrValues.put("name", cityNames[random.nextInt(0, cityNames.length)]);
            tm.addObject(td, attrValues);
        }
    }

    public static void searchObjects(TableDescription td){
        Condition cond1 = new Condition("name", "contains", "on");
        Condition  cond2 = new Condition("capacity", "<", 50000);
        ArrayList<Condition> conditionList = new ArrayList<>();
        conditionList.add(cond1);
        conditionList.add(cond2);

        ArrayList<Map<String, Object>> map = tm.searchObject(td, conditionList);
        Map<String, Object>[] canByStringify = map.toArray(new Map[0]);
        System.out.println(Arrays.toString(canByStringify));
    }

    public static void deleteObjects(TableDescription td){
        Condition cond1 = new Condition("name", "contains", "on");
        Condition  cond2 = new Condition("capacity", "<", 50000);
        ArrayList<Condition> conditionList = new ArrayList<>();
        conditionList.add(cond1);
        conditionList.add(cond2);

        tm.deleteObject(td, conditionList);
    }
}
