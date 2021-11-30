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
    private final List<Event> registeredEvents = new LinkedList<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition tupleAdded = lock.newCondition();

    public CentralizedLinda() {
    }

    @Override
    public void write(Tuple t) {
        lock.lock();

        if(!onTupleAdded(t)) {
            // Tuple was not consumed by an Event.
            getAssociatedList(t).addFirst(t);
            tupleAdded.signalAll();
        }

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

        if(!tuplesByLength.containsKey(template.size())) {
            lock.unlock();
            return null;
        }

        Tuple foundT = null;
        LinkedList<Tuple> tupleList = getAssociatedList(template);
        for (int i = 0; i < tupleList.size(); i++) {
            Tuple t = tupleList.get(i);
            if(!t.matches(template)) {
                continue;
            }

            tupleList.remove(t);
            foundT = t;
            break;
        }

        lock.unlock();
        return foundT;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        lock.lock();

        Tuple foundT = tryTake(template);
        if(foundT != null) {
            // We must add foundT back because this is a read.
            // This also guarantees that accessed items are stored first.
            getAssociatedList(foundT).addFirst(foundT);
        }

        lock.unlock();
        return foundT;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        lock.lock();

        if(!tuplesByLength.containsKey(template.size())) {
            lock.unlock();
            return null;
        }

        LinkedList<Tuple> tupleList = getAssociatedList(template);
        LinkedList<Tuple> foundTuples = new LinkedList<>();

        for (Tuple t : tupleList) {
            if (t.matches(template)) {
                foundTuples.add(t);
            }
        }

        for (Tuple t : foundTuples) {
            tupleList.remove(t);
        }

        if(tupleList.isEmpty()) {
            tuplesByLength.remove(template.size());
        }

        lock.unlock();
        return foundTuples;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        lock.lock();
        Collection<Tuple> foundTuples = takeAll(template);

        if(foundTuples.size() > 0) {
            LinkedList<Tuple> tupleList = getAssociatedList(template);
            for (Tuple t : foundTuples) {
                tupleList.addFirst(t);
            }
        }


        lock.unlock();
        return foundTuples;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        lock.lock();

        boolean registerEvent = true;
        Event ev = new Event(mode, template, callback);
        if(timing == eventTiming.IMMEDIATE && tuplesByLength.containsKey(template.size())) {
            LinkedList<Tuple> tuples = getAssociatedList(template);
            for (Tuple t : tuples) {
                if (!checkEvent(ev, t)) {
                    continue;
                }

                // Found a tuple that fired the event.
                // Event should not be registered, and tuple must be removed if eventMode is TAKE.
                registerEvent = false;
                if (ev.mode == eventMode.TAKE) {
                    tuples.remove(t);
                }
                break;
            }
        }

        if(registerEvent) {
            registeredEvents.add(ev);
        }

        lock.unlock();
    }

    @Override
    public void debug(String prefix) {
        System.out.println(prefix + (formatTuples() + formatEvents()).replaceAll("\n", "\n"+prefix));
    }

    // Internal functions

    /** Called when a Tuple is written to Linda.
     * @param t Tuple to write
     * @return whether an event consumed the Tuple.
     */
    private boolean onTupleAdded(Tuple t) {
        // Executed in write lock context
        for (int i = 0; i < registeredEvents.size(); i++) {
            Event ev = registeredEvents.get(i);
            if(!checkEvent(ev, t)) {
                continue;
            }

            registeredEvents.remove(ev);
            if(ev.mode == eventMode.TAKE) {
                // This tuple is no longer available because EventMode is TAKE.
                return true;
            }
        }

        return false;
    }

    /** Checks if a tuple can fire an event
     * @param ev Event to check
     * @param t Tuple reference
     * @return whether t matches event template (and thus event is fired).
     */
    private boolean checkEvent(Event ev, Tuple t) {
        if (!ev.template.contains(t)) {
            return false;
        }

        ev.callback.call(t);
        return true;
    }

    private LinkedList<Tuple> getAssociatedList(Tuple t) {
        if(!tuplesByLength.containsKey(t.size())) {
            tuplesByLength.put(t.size(), new LinkedList<>());
        }

        return tuplesByLength.get(t.size());
    }

    class Event {
        private final eventMode mode;
        private final Tuple template;
        private final Callback callback;

        public Event(eventMode mode, Tuple template, Callback callback) {
            this.mode = mode;
            this.template = template;
            this.callback = callback;
        }

        public eventMode getMode() {
            return mode;
        }

        public Tuple getTemplate() {
            return template;
        }

        public Callback getCallback() {
            return callback;
        }

        @Override
        public String toString() {
            return "Event{" +
                    "mode=" + mode +
                    ", template=" + template +
                    '}';
        }
    }

    private String formatTuples() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tuples in kernel: \n");

        for (int size : tuplesByLength.keySet()) {
            sb.append("\tTaille ").append(size).append(":\n");
            for (Tuple t : tuplesByLength.get(size)) {
                sb.append("\t\t").append(t).append("\n");
            }
        }

        return sb.toString();
    }

    private String formatEvents() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nEvenements:\n");

        for (Event ev : registeredEvents) {
            sb.append("\t").append(ev).append('\n');
        }

        return sb.toString();
    }

}
