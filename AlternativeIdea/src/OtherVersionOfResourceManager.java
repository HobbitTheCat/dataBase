import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Система управления ресурсами с предотвращением дедлоков и механизмом повторных попыток
 */
public class OtherVersionOfResourceManager {
    // Список доступных ресурсов
    private final Resource[] resources;
    // Граф зависимостей для отслеживания ожиданий
    private final Map<Thread, Set<Resource>> threadToResourcesHeld;
    private final Map<Thread, Set<Resource>> threadToResourcesWaiting;
    // Для синхронизации доступа к графу
    private final Lock graphLock = new ReentrantLock();
    // Условие для оповещения ожидающих потоков
    private final Condition resourceReleased = graphLock.newCondition();

    public OtherVersionOfResourceManager(int numResources) {
        resources = new Resource[numResources];
        for (int i = 0; i < numResources; i++) {
            resources[i] = new Resource(i);
        }
        threadToResourcesHeld = new HashMap<>();
        threadToResourcesWaiting = new HashMap<>();
    }

    /**
     * Класс, представляющий ресурс
     */
    public class Resource {
        private final int id;
        private Thread owner = null;
        private final Lock lock = new ReentrantLock();

        public Resource(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public Thread getOwner() {
            return owner;
        }

        @Override
        public String toString() {
            return "Resource-" + id;
        }
    }

    /**
     * Запрос на получение нескольких ресурсов с предотвращением дедлока и повторными попытками
     * @param requestedResources список требуемых ресурсов
     * @param maxRetries максимальное количество повторных попыток (-1 для бесконечных попыток)
     * @param retryDelayMs задержка между попытками в миллисекундах
     * @return true, если ресурсы были успешно получены, false в случае исчерпания числа попыток
     * @throws InterruptedException если поток был прерван во время ожидания
     */
    public boolean acquireResources(List<Resource> requestedResources, int maxRetries, long retryDelayMs)
            throws InterruptedException {
        Thread currentThread = Thread.currentThread();

        // Сортируем ресурсы по ID для предотвращения дедлока (стратегия упорядочивания ресурсов)
        Collections.sort(requestedResources, Comparator.comparingInt(Resource::getId));

        int retries = 0;
        boolean acquired = false;

        while (!acquired && (maxRetries == -1 || retries <= maxRetries)) {
            if (retries > 0) {
                System.out.println(currentThread.getName() + ": повторная попытка #" + retries +
                        " получить ресурсы " + formatResourceIds(requestedResources));
            }

            acquired = tryAcquireResources(requestedResources);

            if (!acquired) {
                if (maxRetries == -1 || retries < maxRetries) {
                    // Ждем сигнала от других потоков об освобождении ресурсов или используем таймаут
                    graphLock.lock();
                    try {
                        // Регистрируем поток как ожидающий эти ресурсы
                        threadToResourcesWaiting.putIfAbsent(currentThread, new HashSet<>());
                        threadToResourcesWaiting.get(currentThread).addAll(requestedResources);

                        // Ждем сигнал, но с таймаутом
                        resourceReleased.await(retryDelayMs, TimeUnit.MILLISECONDS);

                        // Удаляем из списка ожидания для корректной работы алгоритма обнаружения циклов
                        threadToResourcesWaiting.get(currentThread).removeAll(requestedResources);
                    } finally {
                        graphLock.unlock();
                    }
                    retries++;
                } else {
                    // Исчерпали все попытки
                    break;
                }
            }
        }

        return acquired;
    }

    /**
     * Упрощенная версия метода для получения ресурсов с бесконечными попытками
     */
    public boolean acquireResources(List<Resource> requestedResources) throws InterruptedException {
        return acquireResources(requestedResources, -1, 500);
    }

    /**
     * Упрощенная версия метода для получения ресурсов с ограниченным числом попыток
     */
    public boolean acquireResourcesWithTimeout(List<Resource> requestedResources, int maxRetries, long timeoutMs)
            throws InterruptedException {
        return acquireResources(requestedResources, maxRetries, timeoutMs / (maxRetries + 1));
    }

    /**
     * Внутренний метод для однократной попытки получения всех ресурсов
     */
    private boolean tryAcquireResources(List<Resource> requestedResources) {
        Thread currentThread = Thread.currentThread();
        boolean checkPassed = false;

        graphLock.lock();
        try {
            // Регистрируем поток, если он еще не зарегистрирован
            threadToResourcesHeld.putIfAbsent(currentThread, new HashSet<>());

            // Проверяем, не образуется ли цикл (дедлок) если добавим эти ожидания
            if (wouldFormCycle(currentThread, requestedResources)) {
                // Если образуется цикл, отказываем в запросе
                return false;
            }
            checkPassed = true;
        } finally {
            graphLock.unlock();
        }

        if (!checkPassed) {
            return false;
        }

        boolean acquired = true;
        List<Resource> acquiredResources = new ArrayList<>();

        try {
            // Пытаемся захватить ресурсы по одному
            for (Resource resource : requestedResources) {
                resource.lock.lock();
                if (resource.owner != null && resource.owner != currentThread) {
                    // Ресурс уже занят другим потоком
                    acquired = false;
                    break;
                }
                resource.owner = currentThread;
                acquiredResources.add(resource);
            }
        } finally {
            if (!acquired) {
                // Если не удалось получить все ресурсы, освобождаем уже полученные
                for (Resource resource : acquiredResources) {
                    resource.owner = null;
                    resource.lock.unlock();
                }
            } else {
                // Освобождаем только блокировки, сохраняя владение ресурсами
                for (Resource resource : requestedResources) {
                    resource.lock.unlock();
                }
            }

            graphLock.lock();
            try {
                if (acquired) {
                    threadToResourcesHeld.get(currentThread).addAll(requestedResources);
                }
            } finally {
                graphLock.unlock();
            }
        }

        return acquired;
    }

    /**
     * Освобождение ресурсов
     * @param resourcesToRelease список ресурсов для освобождения
     */
    public void releaseResources(List<Resource> resourcesToRelease) {
        Thread currentThread = Thread.currentThread();

        graphLock.lock();
        try {
            // Проверяем, владеет ли текущий поток этими ресурсами
            Set<Resource> heldResources = threadToResourcesHeld.getOrDefault(currentThread, new HashSet<>());

            for (Resource resource : resourcesToRelease) {
                if (heldResources.contains(resource)) {
                    resource.lock.lock();
                    try {
                        if (resource.owner == currentThread) {
                            resource.owner = null;
                            heldResources.remove(resource);
                        }
                    } finally {
                        resource.lock.unlock();
                    }
                }
            }

            // Оповещаем все ожидающие потоки, что ресурсы освобождены
            resourceReleased.signalAll();
        } finally {
            graphLock.unlock();
        }
    }

    /**
     * Проверяет, образуется ли цикл (дедлок) при добавлении новых ожиданий
     * @param requestingThread поток, запрашивающий ресурсы
     * @param requestedResources запрашиваемые ресурсы
     * @return true, если будет образован цикл
     */
    private boolean wouldFormCycle(Thread requestingThread, List<Resource> requestedResources) {
        // Создаем временную копию графа с новыми ожиданиями
        Map<Thread, Set<Thread>> waitForGraph = buildWaitForGraph();

        // Для каждого запрашиваемого ресурса проверяем, не занят ли он другим потоком
        for (Resource resource : requestedResources) {
            if (resource.owner != null && resource.owner != requestingThread) {
                // Добавляем ребро в граф: текущий поток ждет владельца ресурса
                waitForGraph.putIfAbsent(requestingThread, new HashSet<>());
                waitForGraph.get(requestingThread).add(resource.owner);
            }
        }

        // Проверяем наличие цикла в графе
        return hasCycle(waitForGraph);
    }

    /**
     * Строит граф ожиданий между потоками (кто кого ждет)
     * @return граф ожиданий в виде Map<Thread, Set<Thread>>
     */
    private Map<Thread, Set<Thread>> buildWaitForGraph() {
        Map<Thread, Set<Thread>> waitForGraph = new HashMap<>();

        // Для каждого потока проверяем, какие ресурсы он ожидает
        for (Map.Entry<Thread, Set<Resource>> entry : threadToResourcesWaiting.entrySet()) {
            Thread waitingThread = entry.getKey();
            Set<Resource> waitingResources = entry.getValue();

            waitForGraph.putIfAbsent(waitingThread, new HashSet<>());

            // Для каждого ожидаемого ресурса, если у него есть владелец,
            // добавляем ребро: ожидающий поток -> владелец ресурса
            for (Resource resource : waitingResources) {
                if (resource.owner != null && resource.owner != waitingThread) {
                    waitForGraph.get(waitingThread).add(resource.owner);
                }
            }
        }

        // Добавляем ребра на основе удерживаемых ресурсов
        for (Map.Entry<Thread, Set<Resource>> entry : threadToResourcesHeld.entrySet()) {
            Thread holdingThread = entry.getKey();
            for (Map.Entry<Thread, Set<Resource>> waitEntry : threadToResourcesWaiting.entrySet()) {
                Thread waitingThread = waitEntry.getKey();
                Set<Resource> waitingResources = waitEntry.getValue();

                // Проверяем, не ждет ли какой-то поток ресурсы, удерживаемые текущим потоком
                for (Resource waitingResource : waitingResources) {
                    if (waitingResource.owner == holdingThread) {
                        waitForGraph.putIfAbsent(waitingThread, new HashSet<>());
                        waitForGraph.get(waitingThread).add(holdingThread);
                    }
                }
            }
        }

        return waitForGraph;
    }

    /**
     * Проверяет наличие цикла в графе ожиданий
     * @param graph граф ожиданий
     * @return true, если найден цикл
     */
    private boolean hasCycle(Map<Thread, Set<Thread>> graph) {
        Set<Thread> visited = new HashSet<>();
        Set<Thread> recStack = new HashSet<>();

        // Проверяем каждую вершину графа
        for (Thread thread : graph.keySet()) {
            if (isCyclicUtil(thread, visited, recStack, graph)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Вспомогательная функция для поиска цикла в графе (DFS)
     */
    private boolean isCyclicUtil(Thread thread, Set<Thread> visited,
                                 Set<Thread> recStack, Map<Thread, Set<Thread>> graph) {
        // Если поток еще не был посещен
        if (!visited.contains(thread)) {
            visited.add(thread);
            recStack.add(thread);

            // Проверяем все потоки, которых ждет текущий поток
            Set<Thread> waitingFor = graph.getOrDefault(thread, new HashSet<>());
            for (Thread waitThread : waitingFor) {
                // Если поток еще не посещен и в подграфе есть цикл
                if (!visited.contains(waitThread) &&
                        isCyclicUtil(waitThread, visited, recStack, graph)) {
                    return true;
                }
                // Если поток уже в стеке рекурсии, значит нашли цикл
                else if (recStack.contains(waitThread)) {
                    return true;
                }
            }
        }

        // Удаляем поток из текущего пути
        recStack.remove(thread);
        return false;
    }

    /**
     * Вспомогательный метод для форматирования списка ID ресурсов
     */
    private String formatResourceIds(List<Resource> resources) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < resources.size(); i++) {
            sb.append(resources.get(i).getId());
            if (i < resources.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}

/**
 * Пример использования ResourceManager
 */
class TestDemo {
    public static void main(String[] args) {
        // Создаем менеджер с 5 ресурсами
        final OtherVersionOfResourceManager manager = new OtherVersionOfResourceManager(5);
        final OtherVersionOfResourceManager.Resource[] resources = new OtherVersionOfResourceManager.Resource[5];

        for (int i = 0; i < 5; i++) {
            resources[i] = manager.new Resource(i);
        }

        // Создаем и запускаем потоки
        Thread thread1 = new Thread(() -> {
            List<OtherVersionOfResourceManager.Resource> needed = Arrays.asList(resources[0], resources[1], resources[2]);
            System.out.println("Поток 1: запрашивает ресурсы 0, 1, 2");
            try {
                boolean acquired = manager.acquireResourcesWithTimeout(needed, 3, 5000);
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
                    System.out.println("Поток 1: не удалось получить ресурсы после всех попыток");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "Поток-1");

        Thread thread2 = new Thread(() -> {
            // Специально запрашиваем в другом порядке для демонстрации
            List<OtherVersionOfResourceManager.Resource> needed = Arrays.asList(resources[2], resources[3], resources[1]);
            System.out.println("Поток 2: запрашивает ресурсы 2, 3, 1");
            try {
                // Бесконечные попытки с интервалом по умолчанию
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
                    System.out.println("Поток 2: не удалось получить ресурсы (невозможная ситуация при бесконечных попытках)");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "Поток-2");

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