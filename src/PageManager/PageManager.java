package PageManager;

import Pages.Page;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Name of class: PageManager
 * <p>
 * Description: Prevents from deadlock situations using graph algorithms, watch dogs, and sorts pages by index.
 * <p>
 * Version: 3.0
 * <p>
 * Date 03/19
 * <p>
 * Copyright: Semenov Egor
 */

public class PageManager {
    private final Map<Thread, Set<Page>> threadToResourcesHeld;
    private final Map<Thread, Set<Page>> threadToResourcesWaiting;

    // To synchronize access to the graph
    private final Lock graphLock = new ReentrantLock();

    // Condition for notification of waiting flows
    private final Condition resourceReleased = graphLock.newCondition();
    public final MemoryManager memoryManager;

    public PageManager(MemoryManager memoryManager) {
        this.threadToResourcesHeld = new HashMap<>();
        this.threadToResourcesWaiting = new HashMap<>();
        this.memoryManager = memoryManager;
    }

    /**
     * Request for multiple resources with deadlock and retry prevention
     * @param requestedResourcesIndexes list of requested page indexes
     * @param maxRetries maximum number of retries (-1 for infinite retries)
     * @param retryDelayMs delay between retries in milliseconds
     * @return List<Page> which can be used later on higher levels
     * @throws InterruptedException if the thread was interrupted while waiting
     */
    public List<Page> acquireResources(List<Integer> requestedResourcesIndexes, int maxRetries, long retryDelayMs) throws InterruptedException{
        List<Page> requestedResources = this.memoryManager.indexesToPages(requestedResourcesIndexes);
        if (this.acquireResourcePrivate(requestedResources, maxRetries, retryDelayMs)) return requestedResources;
        return null;
    }

    private boolean acquireResourcePrivate(List<Page> requestedResources, int maxRetries, long retryDelayMs) throws InterruptedException {
        Thread currentThread = Thread.currentThread(); //save the current thread

        // Sort resources by ID to prevent deadlock (resource ordering strategy)
        requestedResources.sort(Comparator.comparingInt(Page::getPageNumber));

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
                        // this.threadToResourcesWaiting.get(currentThread).removeAll(requestedResources);
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
    public List<Page> acquireResources(List<Integer> requestedResourcesIndexes) throws InterruptedException {
        return this.acquireResources(requestedResourcesIndexes, -1, 500);
    }

    /**
     * Simplified version of the method for obtaining resources with a limited number of attempts
     */
    public List<Page> acquireResourcesWithTimeout(List<Integer> requestedResourcesIndexes, int maxRetries, long timeoutMs)
            throws InterruptedException {
        return this.acquireResources(requestedResourcesIndexes, maxRetries, timeoutMs / (maxRetries + 1));
    }

    /**
     * Internal method for a one-time attempt to obtain all resources
     */
    private boolean tryAcquireResources(List<Page> requestedPages) {
        Thread currentThread = Thread.currentThread();

        this.graphLock.lock();
        try {
            // Register a thread if it is not already registered
            this.threadToResourcesHeld.putIfAbsent(currentThread, new HashSet<>());

            // Checks if a loop (deadlock) will not form if it adds these expectations
            if (this.wouldFormCycle(currentThread, requestedPages)) {
                // If a loop is formed, deny the request
                return false;
            }
        } finally {
            graphLock.unlock();
        }

        boolean acquired = true;
        List<Page> acquiredResources = new ArrayList<>();

        try {
            // Trying to grab resources one at a time
            for (Page page : requestedPages) {
                page.lock();
                if (page.getOwner() != null && page.getOwner() != currentThread) {
                    // The resource has already been taken up by another thread
                    acquired = false;
                    break;
                }
                if (page.getOwner() != currentThread) {
                    // Capture only those resources we don't already own
                    page.setOwner(currentThread);
                    acquiredResources.add(page);
                }
            }
        } finally {
            if (!acquired) {
                // If it was not possible to get all the resources, release the resources already obtained
                for (Page page : acquiredResources) {
                    page.setOwner(null);
                    page.unlock();
                }
            } else {
                // If successful, we release only the locks, retaining possession of the resources
                for (Page page : requestedPages) {
                    page.unlock();
                }
            }
            // adding the received resources to the graph
            if (acquired) {
                this.graphLock.lock();
                try {
                    this.threadToResourcesHeld.get(currentThread).addAll(requestedPages);
                } finally {
                    this.graphLock.unlock();
                }
            }
        }

        return acquired;
    }

    /**
     * Resource zone expansion - obtaining additional resources by holding the existing ones
     * @param additionalPagesIndexes list of additional resources to get
     * @param maxRetries maximum number of retries
     * @param retryDelayMs delay between retries in milliseconds
     * @return List of additional pages if additional resources were successfully retrieved
     * @throws InterruptedException if the thread was interrupted while waiting
     */
    public List<Page> expandResourceZone(List<Integer> additionalPagesIndexes, int maxRetries, long retryDelayMs)
            throws InterruptedException {
        List<Page> additionalPages = this.memoryManager.indexesToPages(additionalPagesIndexes);

        Thread currentThread = Thread.currentThread();
        List<Page> actuallyNeeded = new ArrayList<>();

        this.graphLock.lock();
        Set<Page> currentlyHeld;
        try {
            // Check if the thread already has any resources
            currentlyHeld = this.threadToResourcesHeld.getOrDefault(currentThread, new HashSet<>());
            if (currentlyHeld.isEmpty()) {
                System.out.println(currentThread.getName() + ": attempting to expand the zone without owning the resources");
                return null; // exception may be needed
            }
//            System.out.println("Currently held: " + currentlyHeld + "\n");
            // Remove from the list of requested resources those resources that the thread is already holding

            for (Page page : additionalPages) {
                if (!currentlyHeld.contains(page)) {
                    actuallyNeeded.add(page);
                }
            }

            if (actuallyNeeded.isEmpty()) {
                // All requested resources already belong to the stream
                return additionalPages;
            }

            // Sort by ID to prevent deadlocks
            actuallyNeeded.sort(Comparator.comparingInt(Page::getPageNumber));

            System.out.println(currentThread.getName() + ": zone expansion, additional resources required " +
                    this.formatResourceIds(actuallyNeeded));
        } finally {
            this.graphLock.unlock();
        }

        // Use the standard method to obtain additional resources
        if(this.acquireResourcePrivate(actuallyNeeded, maxRetries, retryDelayMs)) return additionalPages;
        return null;
    }

    /**
     * Simplified version of the resource zone extension method with infinite attempts
     */
    public List<Page> expandResourceZone(List<Integer> additionalResourcesIndexes) throws InterruptedException {
        return this.expandResourceZone(additionalResourcesIndexes, -1, 500);
    }



    public void releasePages(List<Integer> resourcesToReleaseIndexes){
        List<Page> resourcesToRelease = this.memoryManager.indexesToPages(resourcesToReleaseIndexes);
        this.releasePagesInternal(resourcesToRelease);
    }
    /**
     * Resource Release
     * @param resourcesToRelease list of page indexes to release
     */
    private void releasePagesInternal(List<Page> resourcesToRelease) {
        Thread currentThread = Thread.currentThread();

        // Create a copy of the list to avoid problems during modification
        List<Page> resourceCopy = new ArrayList<>(resourcesToRelease);

        // Sort resources by ID to maintain the same order of locks capture
        resourceCopy.sort(Comparator.comparingInt(Page::getPageNumber));

        // First release all resource locks
        for (Page page : resourceCopy) {
//            resource.lock.lock();
            try {
                if (page.getOwner() == currentThread) {
                    page.setOwner(null);
                }
                if (page.isDirty()) this.memoryManager.markDirty(page.getPageNumber());
            } finally {
//                resource.lock.unlock();
            }
        }

        // Update the data structures under graphLock
        this.graphLock.lock();
        try {
            Set<Page> heldResources = this.threadToResourcesHeld.get(currentThread);
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
        this.memoryManager.flushAll();
    }

    /**
     * Release of all resources held by the current flow
     */
    public void releaseAllPages() {
        Thread currentThread = Thread.currentThread();

        this.graphLock.lock();
        try {
            List<Page> resourcesToRelease = this.getHeldResources();
            if (!resourcesToRelease.isEmpty()) {
                System.out.println(currentThread.getName() + ": frees up all resources " + this.formatResourceIds(resourcesToRelease));
                this.releasePagesInternal(resourcesToRelease);
            }
        } finally {
            graphLock.unlock();
        }
    }

    public void deletePages(List<Integer> pageToDeleteIndexes){
        List<Page> pagesToDelete = this.memoryManager.indexesToPages(pageToDeleteIndexes);
        Thread currentThread = Thread.currentThread();

        for(Page page : pagesToDelete){
            if(page.getOwner() != currentThread) throw new RuntimeException("Trying to delete page " + page.getPageNumber() + " that doesn't belong to this thread");
        }
        for(Page page : pagesToDelete){
            this.memoryManager.deletePage(page);
        }
        this.releasePagesInternal(pagesToDelete);
    }

    public void exchangePage(Page oldPage, Page newPage) {
        Thread currentThread = Thread.currentThread();
        if (oldPage.getOwner() != currentThread) {
            throw new RuntimeException("Trying to exchange page " + oldPage.getPageNumber() +
                    " that doesn't belong to this thread");
        }
        this.graphLock.lock();
        try {
            Set<Page> heldResources = this.threadToResourcesHeld.get(currentThread);
            if (heldResources != null) {
                heldResources.remove(oldPage);
                heldResources.add(newPage);
            }
            newPage.setOwner(currentThread);
            oldPage.setOwner(null);
            this.memoryManager.exchangePage(newPage);
        } finally {
            this.graphLock.unlock();
        }
    }

    /**
     * Checks if a loop (deadlock) is formed when new waits are added
     * @param requestingThread thread requesting resources
     * @param requestedPages requested resources
     * @return true if a loop is formed
     */
    private boolean wouldFormCycle(Thread requestingThread, List<Page> requestedPages) {
        // Create a temporary copy of the graph with new expectations
        Map<Thread, Set<Thread>> waitForGraph = this.buildWaitForGraph();

        // For each requested resource we check if it is not occupied by another thread
        for (Page page : requestedPages) {
            if (page.getOwner() != null && page.getOwner() != requestingThread) {
                // Add an edge to the graph: the current thread is waiting for the owner of the resource
                waitForGraph.putIfAbsent(requestingThread, new HashSet<>());
                waitForGraph.get(requestingThread).add(page.getOwner());
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
        for (Map.Entry<Thread, Set<Page>> entry : this.threadToResourcesWaiting.entrySet()) {
            Thread waitingThread = entry.getKey();
            Set<Page> waitingPages = entry.getValue();

            waitForGraph.putIfAbsent(waitingThread, new HashSet<>());

            // For each pending resource, if it has an owner,
            // add an edge: waiting thread -> resource owner
            for (Page page : waitingPages) {
                if (page.getOwner()!= null && page.getOwner() != waitingThread) {
                    waitForGraph.get(waitingThread).add(page.getOwner());
                }
            }
        }

        // Add edges based on retained resources
        for (Map.Entry<Thread, Set<Page>> entry : this.threadToResourcesHeld.entrySet()) {
            Thread holdingThread = entry.getKey();
            for (Map.Entry<Thread, Set<Page>> waitEntry : threadToResourcesWaiting.entrySet()) {
                Thread waitingThread = waitEntry.getKey();
                Set<Page> waitingResources = waitEntry.getValue();

                // Check if some thread is waiting for resources held by the current thread
                for (Page waitingResource : waitingResources) {
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
    public List<Page> getHeldResources() {
        Thread currentThread = Thread.currentThread();
        graphLock.lock();
        try {
            Set<Page> heldResources = this.threadToResourcesHeld.getOrDefault(currentThread, new HashSet<>());
            return new ArrayList<>(heldResources);
        } finally {
            graphLock.unlock();
        }
    }

    /**
     * Auxiliary method for formatting the list of resource IDs
     */
    private String formatResourceIds(List<Page> resources) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < resources.size(); i++) {
            sb.append(resources.get(i).getPageNumber());
            if (i < resources.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
