import java.util.Arrays;
import java.util.List;

/**
 * Пример использования ResourceManager
 */
public class DeadlockPreventionDemo {
    public static void main(String[] args) {
        // Создаем менеджер с 5 ресурсами
        ResourceManager manager = new ResourceManager(5);
        ResourceManager.Resource[] resources = new ResourceManager.Resource[5];

        for (int i = 0; i < 5; i++) {
            resources[i] = manager.new Resource(i);
        }

        // Создаем и запускаем потоки
        Thread thread1 = new Thread(() -> {
            List<ResourceManager.Resource> needed = Arrays.asList(resources[0], resources[1], resources[2]);
            System.out.println("Поток 1: запрашивает ресурсы 0, 1, 2");
            boolean acquired = manager.acquireResources(needed);
            if (acquired) {
                System.out.println("Поток 1: получил ресурсы 0, 1, 2");
                try {
                    Thread.sleep(2000); // Имитация работы с ресурсами
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Поток 1: освобождает ресурсы 0, 1, 2");
                manager.releaseResources(needed);
            } else {
                System.out.println("Поток 1: не удалось получить ресурсы (предотвращение дедлока)");
            }
        });

        Thread thread2 = new Thread(() -> {
            // Специально запрашиваем в другом порядке для демонстрации
            List<ResourceManager.Resource> needed = Arrays.asList(resources[2], resources[3], resources[1]);
            System.out.println("Поток 2: запрашивает ресурсы 2, 3, 1");
            boolean acquired = manager.acquireResources(needed);
            if (acquired) {
                System.out.println("Поток 2: получил ресурсы 2, 3, 1");
                try {
                    Thread.sleep(1000); // Имитация работы с ресурсами
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Поток 2: освобождает ресурсы 2, 3, 1");
                manager.releaseResources(needed);
            } else {
                System.out.println("Поток 2: не удалось получить ресурсы (предотвращение дедлока)");
            }
        });

        // Запускаем потоки
        thread1.start();

        // Небольшая пауза, чтобы поток 1 успел начать
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        thread2.start();

        // Ждем завершения потоков
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Демонстрация завершена");
    }
}
