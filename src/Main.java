import Pages.*;
import TableManager.TableDescription;

public class Main {
    public static void main(String[] args) {
//        stringPageTest();
//        objectPageTest();
        metaPageTest();
    }

    public static void metaPageTest(){
        TableDescription dc1 = new TableDescription(1, "City", new String[]{"name", "capacity", "country"}, new int[]{2,3,4});
        TableDescription dc2 = new TableDescription(5, "Person", new String[]{"name", "age", "gender"}, new int[]{6,7,8});
        MetaPage mp =  new MetaPage(0);
        System.out.println(mp);
        mp.add(dc1);
        mp.add(dc2);
        System.out.println(mp);
        System.out.println(mp.getClassByName("City"));
        mp.deleteClassByName("City");
        System.out.println(mp);
        System.out.println(mp.getClassByName("Person"));
        mp.add(dc1);
        System.out.println(mp);
    }

    public static void objectPageTest(){
        ObjectPage op = new ObjectPage((short) 5, 11);
//        System.out.println(op);
        int index = op.append(new Address[] {new Address(2,0), new Address(3, 10),
                new Address(4, 20), new Address(5, 3), new Address(6, 15),});
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