package linda.shm;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {

    private final Map<Integer, LinkedList<Tuple>> tuplesByLength = new HashMap<>();
    private final LinkedList<Event> registeredEvents = new LinkedList<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final LinkedList<Pair<Tuple, Condition>> readConditions = new LinkedList<>();
    private final LinkedList<Pair<Tuple, Condition>> takeConditions = new LinkedList<>();
    //private final Condition tupleAdded = lock.newCondition();

    public CentralizedLinda() {
    }

    @Override
    public void write(Tuple t) {
        lock.lock();

        getAssociatedList(t).addFirst(t);
        onTupleAdded(t);

        lock.unlock();
    }

    @Override
    public Tuple take(Tuple template) {
        lock.lock();
        Tuple t = null;

        try {
            if ((t = tryTake(template)) == null) {
                Condition condition = lock.newCondition();
                takeConditions.addLast(new Pair<>(template, condition));
                condition.await();
            }

            t = tryTake(template);
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
            if ((t = tryRead(template)) == null) {
                Condition condition = lock.newCondition();
                readConditions.addLast(new Pair<>(template, condition));
                condition.await();
            }

            t = tryRead(template);
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

            removeTuple(t);
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
            removeTuple(t);
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

    /** Registers and binds and event to a template
     * Adds mode.TAKE to the front of the list, mode.READ to the end of the list
     * @param mode read or take mode.
     * @param timing (potentially) immediate or only future firing.
     * @param template the filtering template.
     * @param callback the callback to call if a matching tuple appears.
     */
    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        lock.lock();

        boolean registerEvent = true;
        Event ev = new Event(mode, template, callback);
        if(timing == eventTiming.IMMEDIATE && tuplesByLength.containsKey(template.size())) {
            LinkedList<Tuple> tuples = getAssociatedList(template);
            for (Tuple t : tuples) {
                if (!canFireEvent(ev, t)) {
                    continue;
                }
                // Found a tuple that can fire the event.
                // Event should not be registered.
                registerEvent = false;
                // Remove tuple if eventMode is TAKE
                if (ev.mode == eventMode.TAKE) {
                    removeTuple(t);
                }
                // Call event
                ev.callback.call(t);
                break;
            }
        }

        if(registerEvent) {
            if(ev.mode == eventMode.READ) {
                registeredEvents.add(ev);
            }
            else {
                registeredEvents.addFirst(ev);
            }
        }

        lock.unlock();
    }

    @Override
    public void debug(String prefix) {
        lock.lock();
        prefix = prefix + " ";
        System.out.println(prefix + (formatTuples() + formatEvents()).replaceAll("\n", "\n"+prefix) + "- DEBUG END");
        lock.unlock();
    }

    // Internal functions

    /** Called when a Tuple is written to Linda.
     * @param t Tuple to write
     */
    private void onTupleAdded(Tuple t) {
        // Un clone des evenements à l'instant actuel puisque les callbacks peuvent ajouter des evenements.
        List<Event> currentEvents = (List<Event>) registeredEvents.clone();

        // Check reads
        for (Pair<Tuple, Condition> p : readConditions.stream()
                .filter(p -> p.getFirst().contains(t))
                .collect(Collectors.toList())) {
            readConditions.remove(p);
            p.getSecond().signal();
        }

        // Check read events
        for (Event ev : currentEvents.stream()
                    .filter(e -> e.mode == eventMode.READ && canFireEvent(e, t))
                    .collect(Collectors.toList())) {
            registeredEvents.remove(ev);
            ev.callback.call(t);
        }


        // Check first take
        Pair<Tuple, Condition> takePair = takeConditions.stream()
                .filter(p -> p.getFirst().contains(t))
                .findFirst().orElse(null);
        if(takePair != null) {
            takeConditions.remove(takePair);
            takePair.getSecond().signal();
            return;
        }

        // Check first take event
        Event takeEvent = currentEvents.stream()
                .filter(e -> e.mode == eventMode.TAKE && canFireEvent(e, t))
                .findFirst().orElse(null);
        if(takeEvent != null) {
            getAssociatedList(t).remove(t);
            registeredEvents.remove(takeEvent);
            takeEvent.callback.call(t);
        }
    }

    /** Checks if a tuple can fire an event
     * @param ev Event to check
     * @param t Tuple reference
     * @return whether t matches event template.
     */
    private boolean canFireEvent(Event ev, Tuple t) {
        return ev.template.contains(t);
    }

    private LinkedList<Tuple> getAssociatedList(Tuple t) {
        if(!tuplesByLength.containsKey(t.size())) {
            tuplesByLength.put(t.size(), new LinkedList<>());
        }

        return tuplesByLength.get(t.size());
    }

    private void removeTuple(Tuple t) {
        LinkedList<Tuple> associatedList = getAssociatedList(t);
        associatedList.remove(t);
        if(associatedList.isEmpty()) {
            tuplesByLength.remove(t.size());
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

    class Pair<T1, T2> {
        private T1 t1;
        private T2 t2;

        public Pair(T1 t1, T2 t2) {
            this.t1 = t1;
            this.t2 = t2;
        }

        public T1 getFirst() {
            return t1;
        }

        public T2 getSecond() {
            return t2;
        }
    }

}
