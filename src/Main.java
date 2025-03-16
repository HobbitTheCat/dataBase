import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import Pages.*;

public class Main {
    public static void main(String[] args) {
//        stringPageTest();
//        objectPageTest();
//        metaPageTest();
    }

    public static void metaPageTest(){
        DataClass dc = new DataClass(1, "City", new String[]{"name", "capacity", "country"}, new int[]{2,3,4});
        MetaPage mp =  new MetaPage(0);
        System.out.println(mp);
        mp.createClass(dc);
        System.out.println(mp);
        DataClass out = mp.getClassByName("City");
        System.out.println(out);
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
        sp.getStringByIndex(0);
        int index = sp.putString("Hello World!", new Address(1, 0));
        int index2 = sp.putString("My database is working", new Address(1, 1));

        String result = sp.getStringByIndex(index);
        Object []  resultArray = sp.getStringByIndexWithMeta(index2);

        System.out.println("Extracted string: " + result);
        System.out.println("Extracted string: {ObjectPage: " + resultArray[0] +  ", Value: " + resultArray[1] + "}");
        System.out.println(sp);

        sp.replaceString(index, "New String", new Address(1, 0));
        System.out.println(sp);

        sp.deleteString(index);
        System.out.println(sp);

        sp.replaceString(index, "New String", new Address(1, 0));
        System.out.println(sp);
    }
}