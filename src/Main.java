import Pages.*;
import TableManager.TableDescription;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
//        stringPageTest();
//        objectPageTest();
        metaPageTest();

//        int[] test = new int[4];
//        int test;
//        System.out.println(test);

//        Boat boat = new Boat("Bismark", 72);
//        System.out.println(getFieldTypesMap(Boat.class));

    }

    public static Map<String, String> getFieldTypesMap(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .collect(Collectors.toMap(Field::getName, field -> field.getType().getSimpleName()));
    }

    public static void metaPageTest(){
        TableDescription dc1 = new TableDescription(1, "City", new String[]{"name", "capacity", "country"}, new int[]{2,3,4});
        TableDescription dc2 = new TableDescription(9, "Car", new String[]{"name", "capacity", "country", "speed"}, new int[]{10,11,12,13});
        TableDescription dc3 = new TableDescription(20, "Crocodile", new String[]{"name", "capacity", "country", "speed", "teethNumber"}, new int[]{14,15,16,17,18});
//        TableDescription dc4 = new TableDescription(5, "Person", new String[]{"name", "age", "gender"}, new int[]{6,7,8});
        MetaPage mp =  new MetaPage(0);
        System.out.println(mp);
        mp.add(dc1);
        mp.add(dc2);
        mp.add(dc3);
        System.out.println("After adding all 3\n" + mp);
        System.out.println("Searched: " + mp.searchTableByName("Crocodile"));
//        System.out.println(mp.getClassByName("City"));

        mp.deleteClassByName("City");
        System.out.println("After deleting City\n" + mp);
        mp.deleteClassByName("Car");
        System.out.println("After deleting Car\n" + mp);
        System.out.println("After deleting City\n" + mp.getClassByName("Crocodile"));
//        mp.add(dc2);
//        System.out.println("After adding Car back\n" + mp);
//        System.out.println(mp.getClassByName("Crocodile"));
    }

    public static void objectPageTest(){
        ObjectPage op = new ObjectPage((short) 1, 11);
//        System.out.println(op);
        int index = op.append(new Address[] {new Address(2,0)}); //, new Address(3, 10),
//                new Address(4, 20), new Address(5, 3), new Address(6, 15),});
        System.out.println(index);
        System.out.println(op);
    }

    public static void stringPageTest(){
        StringPage sp = new StringPage(10);
        sp.get((short) 0);
        short index = sp.add("Hello World!", new Address(1, 0));
        int index2 = sp.add("My database is working", new Address(1, 1));

        String result = sp.get((short) index);
        Object []  resultArray = sp.getStringByIndexWithMeta(index2);

        System.out.println("Extracted string: " + result);
        System.out.println("Extracted string: {ObjectPage: " + resultArray[0] +  ", Value: " + resultArray[1] + "}");
        System.out.println(sp);

        sp.replace(index, "New String", new Address(1, 0));
        System.out.println(sp);

        sp.delete(index);
        System.out.println(sp);

        sp.replace(index, "New String", new Address(1, 0));
        System.out.println(sp);
    }
}

class Boat{
    private String Name;
    private int speed;
    public Boat(String name, int speed) {
        this.Name = name;
        this.speed = speed;
    }
}