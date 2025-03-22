import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Система управления ресурсами с предотвращением дедлоков
 */
public class ResourceManager {
    // Список доступных ресурсов
    private final Resource[] resources;
    // Граф зависимостей для отслеживания ожиданий
    private final Map<Thread, Set<Resource>> threadToResourcesHeld;
    private final Map<Thread, Set<Resource>> threadToResourcesWaiting;
    // Для синхронизации доступа к графу
    private final Lock graphLock = new ReentrantLock();

    public ResourceManager(int numResources) {
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

        @Override
        public String toString() {
            return "Resource-" + id;
        }
    }

    /**
     * Запрос на получение нескольких ресурсов с предотвращением дедлока
     * @param requestedResources список требуемых ресурсов
     * @return true, если ресурсы были успешно получены, false в случае отказа
     */
    public boolean acquireResources(List<Resource> requestedResources) {
        Thread currentThread = Thread.currentThread();

        // Сортируем ресурсы по ID для предотвращения дедлока (стратегия упорядочивания ресурсов)
        Collections.sort(requestedResources, Comparator.comparingInt(Resource::getId));

        graphLock.lock();
        try {
            // Регистрируем поток, если он еще не зарегистрирован
            threadToResourcesHeld.putIfAbsent(currentThread, new HashSet<>());
            threadToResourcesWaiting.putIfAbsent(currentThread, new HashSet<>());

            // Добавляем ожидаемые ресурсы
            threadToResourcesWaiting.get(currentThread).addAll(requestedResources);

            // Проверяем, не образуется ли цикл (дедлок) если добавим эти ожидания
            if (wouldFormCycle(currentThread, requestedResources)) {
                // Если образуется цикл, отказываем в запросе
                threadToResourcesWaiting.get(currentThread).removeAll(requestedResources);
                return false;
            }
        } finally {
            graphLock.unlock();
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
                // Обновляем графы ожиданий и владений
                threadToResourcesWaiting.get(currentThread).removeAll(requestedResources);
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
}