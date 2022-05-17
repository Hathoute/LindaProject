package linda.server;
import linda.AsynchronousCallback;
import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.server.cache.CacheInvalidator;
import linda.server.cache.TupleWrapper;
import linda.server.multiserver.InternalClient;
import linda.shm.CentralizedLinda;
import linda.utils.Helper;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LindaServer extends UnicastRemoteObject implements ServerInterface{
    private Linda myLinda;
    private List<CacheInvalidator> subscribedCaches;
    private List<InternalClient> fallbacks;

    public LindaServer() throws RemoteException {
        myLinda= new CentralizedLinda();
        subscribedCaches = new LinkedList<>();
        fallbacks = new ArrayList<>();
    }

    public void addFallback(String serverURL) {
        fallbacks.add(new InternalClient(serverURL, this::notifyCaches));
    }

    @Override
    public void subscribe(CacheInvalidator cache) throws RemoteException {
        subscribedCaches.add(cache);
    }

    private synchronized void notifyCaches(long uid) {
        List<CacheInvalidator> disconnected = new LinkedList<>();
        for (CacheInvalidator c : subscribedCaches) {
            try {
                c.invalidate(uid);
            } catch (RemoteException re) {
                disconnected.add(c);
            }
        }

        for (CacheInvalidator c : disconnected) {
            subscribedCaches.remove(c);
        }
    }

    @Override
    public void write(Tuple t) throws RemoteException {
        myLinda.write(new TupleWrapper(t, Helper.getNextUniqueId()));
    }

    @Override
    public TupleWrapper take(Tuple template) throws RemoteException {
        TupleWrapper tw = (TupleWrapper) myLinda.tryTake(template);
        boolean notifyCache = true;
        if(tw == null) {
            if (fallbacks.size() > 0) {
                tw = fallbackRoutine(template, Linda.eventMode.TAKE);
                notifyCache = false;
            } else {
                tw = (TupleWrapper) myLinda.take(template);
            }
        }

        if(notifyCache) {
            notifyCaches(tw.getUid());
        }

        return tw;
    }

    @Override
    public TupleWrapper read(Tuple template) throws RemoteException {
        TupleWrapper tw = (TupleWrapper) myLinda.tryRead(template);
        if(tw != null)
            return tw;

        if(fallbacks.size() > 0) {
            return fallbackRoutine(template, Linda.eventMode.READ);
        }
        else {
            return (TupleWrapper) myLinda.read(template);
        }
    }

    private TupleWrapper fallbackRoutine(Tuple template, Linda.eventMode mode) {
        Map<Long, Linda> eventSource = new HashMap<>();
        Lock lock = new ReentrantLock();
        Semaphore semaphore = new Semaphore(0, true);
        final boolean[] finished = {false};
        final TupleWrapper[] ts = {null};
        Callback callback = new Callback() {
            @Override
            public void call(long eventId, Tuple t) {
                lock.lock();
                if(finished[0]) {
                    lock.unlock();
                    return;
                }

                for (Long id : eventSource.keySet()) {
                    if(id != eventId) {
                        eventSource.get(id).unregisterEvent(id);
                    }
                }
                finished[0] = true;
                ts[0] = (TupleWrapper) t;
                lock.unlock();
                semaphore.release(1);
            }
        };
        lock.lock();
        long uniqueId = Helper.getNextUniqueId();
        long localEventId = myLinda.eventRegister(mode, Linda.eventTiming.IMMEDIATE, template, callback);
        eventSource.put(localEventId, myLinda);
        for (InternalClient fallback : fallbacks) {
            long remoteEventId = fallback.eventRegister(uniqueId, mode, Linda.eventTiming.IMMEDIATE, template, callback);
            if(remoteEventId == 0) {
                break;
            }
            eventSource.put(remoteEventId, fallback);
        }
        lock.unlock();
        semaphore.acquireUninterruptibly(1);
        return ts[0];
    }

    @Override
    public TupleWrapper tryTake(Tuple template) throws RemoteException {
        TupleWrapper t = (TupleWrapper) myLinda.tryTake(template);
        if(t != null) {
            notifyCaches(t.getUid());
        }
        return t;
    }

    @Override
    public TupleWrapper tryRead(Tuple template) throws RemoteException {
        return (TupleWrapper) myLinda.tryRead(template);
    }

    @Override
    public Collection<TupleWrapper> takeAll(Tuple template) throws RemoteException {
        Collection<TupleWrapper> ts = Helper.collectionCast(myLinda.takeAll(template));

        ts.parallelStream().forEach(x -> notifyCaches(x.getUid()));

        return ts;
    }

    @Override
    public Collection<TupleWrapper> readAll(Tuple template) throws RemoteException {
        return Helper.collectionCast(myLinda.readAll(template));
    }

    private Map<Long, Map<Long, Linda>> childEvents = new HashMap<>();
    private Set<Long> pending = new HashSet<>();
    private Lock registerLock = new ReentrantLock();

    @Override
    public long eventRegister(long requestId, Linda.eventMode mode, Linda.eventTiming timing, Tuple template, RCallback rcallback) throws RemoteException {
        final long uniqueId = requestId;
        if(pending.contains(uniqueId)) {
            return -1;
        }

        final long[] parentId = {0};
        pending.add(uniqueId);
        Callback callback = new Callback() {
            @Override
            public void call(long eventId, Tuple t) {
                try {
                    registerLock.lock();
                    if(mode == Linda.eventMode.TAKE && parentId[0] == eventId) {
                        notifyCaches(((TupleWrapper) t).getUid());
                    }

                    if(pending.contains(uniqueId)) {
                        pending.remove(uniqueId);
                        myLinda.unregisterEvent(parentId[0]);
                        if(parentId[0] > 0) {
                            Map<Long, Linda> children = childEvents.remove(parentId[0]);
                            children.keySet()
                                    .forEach(x -> children.get(x).unregisterEvent(x));

                        }
                    }
                    else {
                        // Callback already executed, abort...
                        registerLock.unlock();
                        return;
                    }
                    registerLock.unlock();

                    rcallback.call(eventId, t);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };
        AsynchronousCallback cb = new AsynchronousCallback(callback);
        parentId[0] = myLinda.eventRegister(mode, timing, template, cb);
        if(parentId[0] == 0) {
            return 0;
        }

        Map<Long, Linda> childEvents = new HashMap<>();
        this.childEvents.put(parentId[0], childEvents);
        for(InternalClient fallback : fallbacks) {
            long childId = fallback.eventRegister(requestId, mode, timing, template, cb);
            if(childId == -1) {
                // We probably hit a loopback
                continue;
            }
            if(childId == 0) {
                return 0;
            }
            childEvents.put(childId, fallback);
        }

        return parentId[0];
    }

    @Override
    public void unregisterEvent(long eventId) throws RemoteException {
        myLinda.unregisterEvent(eventId);
    }

    @Override
    public void debug(String prefix) throws RemoteException {
        myLinda.debug(prefix);
    }

    /*
    @Override
    public String debug(String prefix) throws RemoteException {
        // Cette approche a été implémentée après avoir essayé de passer un RemoteOutputStream qui
        // contient une methode void write(int) et changer le System.out du serveur avec un PrintStream
        // issu du OutputStream du client, ce qui faisait que l'écriture du debug dans le client soit interrompue
        // par les écritures du client (ex. "SERVEUR\n" + "CLIENT\n" => "SERCLIENT\nVEUR\n"

        PrintStream serverStream = System.out;
        final boolean[] finished = new boolean[1];
        final ArrayList<Integer> writeBuffer = new ArrayList<>();
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                if(b == -1) {
                    finished[0] = true;
                    System.setOut(serverStream);
                    return;
                }

                writeBuffer.add(b);
            }
        };
        PrintStream printStream = new PrintStream(out);
        System.setOut(printStream);
        myLinda.debug(prefix);
        // Signal End of stream
        System.out.write(-1);

        // Wait for System.out.println() to process everything...
        while(!finished[0]) { }

        // Convert from int to byte
        byte[] bytes = new byte[writeBuffer.size()];
        for(int i = 0; i < writeBuffer.size(); i++) {
            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(writeBuffer.get(i));
            bytes[i] = b.get(3);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }*/
}
