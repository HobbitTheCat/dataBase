import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

public class ReflectionTest {
    interface SerializableObject {
        void serialize();
        void deserialize();
    }

    public static void main(String[] args) {
        Human h = new Human("Ivan", 18);
        Class<?> clazz = h.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            System.out.println("Поле: " + field.getName() + " (" + field.getType() + ")");
        }
    }
    public SerializableObject restoreHuman(String className, int age, String name) throws Exception {
        Class<?> clazz = Class.forName(className);
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        SerializableObject object = (SerializableObject) constructor.newInstance();
        object.deserialize();
        return object;
    }
}

class Human{
    private String name;
    private int age;

    public int getAge() {
        return age;
    }
    public String getName() {
        return name;
    }

    public Human(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
