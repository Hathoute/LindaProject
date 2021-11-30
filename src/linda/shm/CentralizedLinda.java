package linda.shm;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {

    private final Map<Integer, LinkedList<Tuple>> tuplesByLength = new HashMap<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition tupleAdded = lock.newCondition();

    public CentralizedLinda() {
    }

    @Override
    public void write(Tuple t) {
        lock.lock();

        getAssociatedList(t).addFirst(t);
        tupleAdded.signalAll();

        lock.unlock();
    }

    @Override
    public Tuple take(Tuple template) {
        lock.lock();
        Tuple t = null;

        try {
            while ((t = tryTake(template)) == null) {
                tupleAdded.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        return t;
    }

    @Override
    public Tuple read(Tuple template) {
        lock.lock();
        Tuple t = null;

        try {
            while ((t = tryRead(template)) == null) {
                tupleAdded.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        return t;
    }

    @Override
    public Tuple tryTake(Tuple template) {
        lock.lock();

        Tuple t = tryRead(template);
        if(t != null) {
            // Tuple found, because this is a take we must delete it
            // We delete the last item, since tryRead updated its position
            List<Tuple> tupleList = getAssociatedList(t);
            tupleList.remove(tupleList.size() - 1);
            if(tupleList.size() == 0) {
                tuplesByLength.remove(t.size());
            }
        }

        lock.unlock();
        return t;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        lock.lock();

        Tuple readT = null;
        LinkedList<Tuple> tupleList = getAssociatedList(template);
        for (int i = 0; i < tupleList.size(); i++) {
            Tuple t = tupleList.get(i);
            if(!t.matches(template)) {
                continue;
            }

            tupleList.remove(t);
            tupleList.addFirst(t);
            readT = t;
            break;
        }

        lock.unlock();
        return readT;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        lock.lock();
        Collection<Tuple> tuples = readAll(template);

        if(tuples.size() > 0) {
            // TODO.
        }

        lock.unlock();
        return tuples;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        lock.lock();
        Collection<Tuple> tuples = new LinkedList<>();

        lock.unlock();
        return tuples;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {

    }

    @Override
    public void debug(String prefix) {

    }

    // Internal functions

    private LinkedList<Tuple> getAssociatedList(Tuple t) {
        if(!tuplesByLength.containsKey(t.size())) {
            tuplesByLength.put(t.size(), new LinkedList<>());
        }

        return tuplesByLength.get(t.size());
    }

}
