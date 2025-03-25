package PreviousMemoryManagers;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  A resource management system with deadlock prevention and the ability to expand the resource zone
 */
public class ResourceManager {
    // Dependency graph for expectation tracking
    private final Map<Thread, Set<Resource>> threadToResourcesHeld;
    private final Map<Thread, Set<Resource>> threadToResourcesWaiting;
    // To synchronize access to the graph
    private final Lock graphLock = new ReentrantLock();
    // Condition for notification of waiting flows
    private final Condition resourceReleased = graphLock.newCondition();
    private final VirtualMemoryManagerV3_0 VMM;

    public ResourceManager(VirtualMemoryManagerV3_0 VMM) {
        this.threadToResourcesHeld = new HashMap<>();
        this.threadToResourcesWaiting = new HashMap<>();
        this.VMM = VMM;
    }

    /**
     * Request for multiple resources with deadlock and retry prevention
     * @param requestedResourcesIndexes list of requested page indexes
     * @param maxRetries maximum number of retries (-1 for infinite retries)
     * @param retryDelayMs delay between retries in milliseconds
     * @return true if the resources were successfully retrieved, false if the number of attempts is exhausted
     * @throws InterruptedException if the thread was interrupted while waiting
     */
    public boolean acquireResources(List<Integer> requestedResourcesIndexes, int maxRetries, long retryDelayMs) throws InterruptedException{
        List<Resource> requestedResources = this.indexesToResources(requestedResourcesIndexes);
        return this.acquireResourcePrivate(requestedResources, maxRetries, retryDelayMs);
    }

    private boolean acquireResourcePrivate(List<Resource> requestedResources, int maxRetries, long retryDelayMs) throws InterruptedException {
        Thread currentThread = Thread.currentThread(); //save the current thread

        // Sort resources by ID to prevent deadlock (resource ordering strategy)
        requestedResources.sort(Comparator.comparingInt(Resource::getId));

        int retries = 0;
        boolean acquired = false; // while haven't gotten

        while (!acquired && (maxRetries == -1 || retries <= maxRetries)) {
            if (retries > 0) {System.out.println(currentThread.getName() + ": retry #" + retries +
                        " to get resources " + this.formatResourceIds(requestedResources));
            }

            acquired = this.tryAcquireResources(requestedResources); //try to get resources, returns status

            if (!acquired) { //if we didn't get
                if (maxRetries == -1 || retries < maxRetries) {
                    // Wait for a signal from other threads to release resources or use timeout
                    this.graphLock.lock();
                    try {
                        // Register the thread as waiting for these resources
                        this.threadToResourcesWaiting.putIfAbsent(currentThread, new HashSet<>());
                        this.threadToResourcesWaiting.get(currentThread).addAll(requestedResources);

                        // Waiting for a signal, but with a timeout.
                        this.resourceReleased.await(retryDelayMs, TimeUnit.MILLISECONDS);

                        // We remove from the waiting list for correct operation of the loop detection algorithm
//                        this.threadToResourcesWaiting.get(currentThread).removeAll(requestedResources);
                        requestedResources.forEach(this.threadToResourcesWaiting.get(currentThread)::remove);
                    } finally {
                        this.graphLock.unlock();
                    }
                    retries++;
                } else {
                    // We've exhausted all attempts
                    break;
                }
            }
        }
        return acquired;
    }

    /**
     * A simplified version of the method for obtaining resources with infinite attempts
     */
    public boolean acquireResources(List<Integer> requestedResourcesIndexes) throws InterruptedException {
        return this.acquireResources(requestedResourcesIndexes, -1, 500);
    }

    /**
     * Simplified version of the method for obtaining resources with a limited number of attempts
     */
    public boolean acquireResourcesWithTimeout(List<Integer> requestedResourcesIndexes, int maxRetries, long timeoutMs)
            throws InterruptedException {
        return this.acquireResources(requestedResourcesIndexes, maxRetries, timeoutMs / (maxRetries + 1));
    }

    /**
     * Internal method for a one-time attempt to obtain all resources
     */
    private boolean tryAcquireResources(List<Resource> requestedResources) {
        Thread currentThread = Thread.currentThread();

        this.graphLock.lock();
        try {
            // Register a thread if it is not already registered
            this.threadToResourcesHeld.putIfAbsent(currentThread, new HashSet<>());

            // Let's check if a loop (deadlock) will not form if we add these expectations
            if (this.wouldFormCycle(currentThread, requestedResources)) {
                // If a loop is formed, deny the request
                return false;
            }
        } finally {
            graphLock.unlock();
        }

        boolean acquired = true;
        List<Resource> acquiredResources = new ArrayList<>();

        try {
            // Trying to grab resources one at a time
            for (Resource resource : requestedResources) {
                resource.lock();
                if (resource.getOwner() != null && resource.getOwner() != currentThread) {
                    // The resource has already been taken up by another thread
                    acquired = false;
                    break;
                }
                if (resource.getOwner() != currentThread) {
                    // Capture only those resources we don't already own
                    resource.setOwner(currentThread);
                    acquiredResources.add(resource);
                }
            }
        } finally {
            if (!acquired) {
                // If it was not possible to get all the resources, release the resources already obtained
                for (Resource resource : acquiredResources) {
                    resource.setOwner(null);
                    resource.unlock();
                }
            } else {
                // If successful, we release only the locks, retaining possession of the resources
                for (Resource resource : requestedResources) {
                    resource.unlock();
                }
            }
            // adding the received resources to the graph
            this.graphLock.lock();
            try {
                if (acquired) {
                    this.threadToResourcesHeld.get(currentThread).addAll(requestedResources);
                }
            } finally {
                this.graphLock.unlock();
            }
        }

        return acquired;
    }

    /**
     * Resource zone expansion - obtaining additional resources by holding the existing ones
     * @param additionalResourcesIndexes list of additional resources to get
     * @param maxRetries maximum number of retries
     * @param retryDelayMs delay between retries in milliseconds
     * @return true if additional resources were successfully retrieved
     * @throws InterruptedException if the thread was interrupted while waiting
     */
    public boolean expandResourceZone(List<Integer> additionalResourcesIndexes, int maxRetries, long retryDelayMs)
            throws InterruptedException {
        List<Resource> additionalResources = this.indexesToResources(additionalResourcesIndexes);

        Thread currentThread = Thread.currentThread();
        List<Resource> actuallyNeeded = new ArrayList<>();

        this.graphLock.lock();
        Set<Resource> currentlyHeld;
        try {
            // Check if the thread already has any resources
            currentlyHeld = this.threadToResourcesHeld.getOrDefault(currentThread, new HashSet<>());
            if (currentlyHeld.isEmpty()) {
                System.out.println(currentThread.getName() + ": attempting to expand the zone without owning the resources");
                return false;
            }

            // Remove from the list of requested resources those resources that the thread is already holding

            for (Resource resource : additionalResources) {
                if (!currentlyHeld.contains(resource)) {
                    actuallyNeeded.add(resource);
                }
            }

            if (actuallyNeeded.isEmpty()) {
                // All requested resources already belong to the stream
                return true;
            }

            // Sort by ID to prevent deadlocks
            actuallyNeeded.sort(Comparator.comparingInt(Resource::getId));

            System.out.println(currentThread.getName() + ": zone expansion, additional resources required " +
                    this.formatResourceIds(actuallyNeeded));
        } finally {
            this.graphLock.unlock();
        }

        // Use the standard method to obtain additional resources
        return this.acquireResourcePrivate(actuallyNeeded, maxRetries, retryDelayMs);
    }

    /**
     * Simplified version of the resource zone extension method with infinite attempts
     */
    public boolean expandResourceZone(List<Integer> additionalResourcesIndexes) throws InterruptedException {
        return this.expandResourceZone(additionalResourcesIndexes, -1, 500);
    }

    /**
     * Resource Release
     * @param resourcesToRelease list of resources to release
     */
    public void releaseResources(List<Resource> resourcesToRelease) {
        Thread currentThread = Thread.currentThread();

        // Create a copy of the list to avoid problems during modification
        List<Resource> resourceCopy = new ArrayList<>(resourcesToRelease);

        // Sort resources by ID to maintain the same order of locks capture
        resourceCopy.sort(Comparator.comparingInt(Resource::getId));

        // First release all resource locks
        for (Resource resource : resourceCopy) {
//            resource.lock.lock();
            try {
                if (resource.getOwner() == currentThread) {
                    resource.setOwner(null);
                }
            } finally {
//                resource.lock.unlock();
            }
        }

        // Update the data structures under graphLock
        this.graphLock.lock();
        try {
            Set<Resource> heldResources = this.threadToResourcesHeld.get(currentThread);
            if (heldResources != null) {
                resourceCopy.forEach(heldResources::remove);
                if (heldResources.isEmpty()) {
                    this.threadToResourcesHeld.remove(currentThread);
                }
                this.resourceReleased.signalAll();
            }
        } finally {
            this.graphLock.unlock();
        }
    }

    /**
     * Release of all resources held by the current flow
     */
    public void releaseAllResources() {
        Thread currentThread = Thread.currentThread();

        this.graphLock.lock();
        try {
            Set<Resource> heldResources = this.threadToResourcesHeld.getOrDefault(currentThread, new HashSet<>());
            List<Resource> resourcesToRelease = new ArrayList<>(heldResources);

            if (!resourcesToRelease.isEmpty()) {
                System.out.println(currentThread.getName() + ": frees up all resources " + this.formatResourceIds(resourcesToRelease));
                this.releaseResources(resourcesToRelease);
            }
        } finally {
            graphLock.unlock();
        }
    }

    private List<Resource> indexesToResources(List<Integer> requestedResourcesIndexes){
        List<Resource> requestedResources = new ArrayList<>(requestedResourcesIndexes.size());
        for (Integer index : requestedResourcesIndexes) {
            if(index != -1){
                Resource resourceByIndex = this.VMM.loadPage(index);
                if(resourceByIndex != null) requestedResources.add(resourceByIndex);
                else throw new IndexOutOfBoundsException("Trying to acquire resource with index " + index + " which is out of bounds.");
            }else{
                Resource resourceByIndex = this.VMM.allocatePage();
                requestedResources.add(resourceByIndex);
            }
        }
        return requestedResources;
    }

    /**
     * Checks if a loop (deadlock) is formed when new waits are added
     * @param requestingThread thread requesting resources
     * @param requestedResources requested resources
     * @return true if a loop is formed
     */
    private boolean wouldFormCycle(Thread requestingThread, List<Resource> requestedResources) {
        // Create a temporary copy of the graph with new expectations
        Map<Thread, Set<Thread>> waitForGraph = this.buildWaitForGraph();

        // For each requested resource we check if it is not occupied by another thread
        for (Resource resource : requestedResources) {
            if (resource.getOwner() != null && resource.getOwner() != requestingThread) {
                // Add an edge to the graph: the current thread is waiting for the owner of the resource
                waitForGraph.putIfAbsent(requestingThread, new HashSet<>());
                waitForGraph.get(requestingThread).add(resource.getOwner());
            }
        }

        // Check if there is a cycle in the graph
        return this.hasCycle(waitForGraph);
    }

    /**
     * Constructs the expectation graph between threads (who is waiting for whom)
     * @return expectation graph as Map<Thread, Set<Thread>>>
     */
    private Map<Thread, Set<Thread>> buildWaitForGraph() {
        Map<Thread, Set<Thread>> waitForGraph = new HashMap<>();

        // For each thread, we check what resources it expects to receive
        for (Map.Entry<Thread, Set<Resource>> entry : this.threadToResourcesWaiting.entrySet()) {
            Thread waitingThread = entry.getKey();
            Set<Resource> waitingResources = entry.getValue();

            waitForGraph.putIfAbsent(waitingThread, new HashSet<>());

            // For each pending resource, if it has an owner,
            // add an edge: waiting thread -> resource owner
            for (Resource resource : waitingResources) {
                if (resource.getOwner()!= null && resource.getOwner() != waitingThread) {
                    waitForGraph.get(waitingThread).add(resource.getOwner());
                }
            }
        }

        // Add edges based on retained resources
        for (Map.Entry<Thread, Set<Resource>> entry : this.threadToResourcesHeld.entrySet()) {
            Thread holdingThread = entry.getKey();
            for (Map.Entry<Thread, Set<Resource>> waitEntry : threadToResourcesWaiting.entrySet()) {
                Thread waitingThread = waitEntry.getKey();
                Set<Resource> waitingResources = waitEntry.getValue();

                // Check if some thread is waiting for resources held by the current thread
                for (Resource waitingResource : waitingResources) {
                    if (waitingResource.getOwner() == holdingThread) {
                        waitForGraph.putIfAbsent(waitingThread, new HashSet<>());
                        waitForGraph.get(waitingThread).add(holdingThread);
                    }
                }
            }
        }

        return waitForGraph;
    }

    /**
     * Checks if there is a cycle in the expectation graph
     * @param graph expectation graph
     * @return true if a cycle is found
     */
    private boolean hasCycle(Map<Thread, Set<Thread>> graph) {
        Set<Thread> visited = new HashSet<>();
        Set<Thread> recStack = new HashSet<>();

        // Check each vertex of the graph
        for (Thread thread : graph.keySet()) {
            if (isCyclicUtil(thread, visited, recStack, graph)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Auxiliary function for finding a loop in a graph (standard DFS)
     */
    private boolean isCyclicUtil(Thread thread, Set<Thread> visited,
                                 Set<Thread> recStack, Map<Thread, Set<Thread>> graph) {
        // If the stream has not yet been visited
        if (!visited.contains(thread)) {
            visited.add(thread);
            recStack.add(thread);

            // Check all threads that the current thread is waiting for
            Set<Thread> waitingFor = graph.getOrDefault(thread, new HashSet<>());
            for (Thread waitThread : waitingFor) {
                // If the thread has not yet been visited and there is a cycle in the subgraph
                if (!visited.contains(waitThread) &&
                        this.isCyclicUtil(waitThread, visited, recStack, graph)) {
                    return true;
                }
                // If the thread is already on the recursion stack, then we have found a loop
                else if (recStack.contains(waitThread)) {
                    return true;
                }
            }
        }

        // Remove a thread from the current path
        recStack.remove(thread);
        return false;
    }

    /**
     * Get list of current resources held by current thread
     */
    public List<Resource> getHeldResources() {
        Thread currentThread = Thread.currentThread();
        graphLock.lock();
        try {
            Set<Resource> heldResources = threadToResourcesHeld.getOrDefault(currentThread, new HashSet<>());
            return new ArrayList<>(heldResources);
        } finally {
            graphLock.unlock();
        }
    }

    /**
     * Auxiliary method for formatting the list of resource IDs
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

