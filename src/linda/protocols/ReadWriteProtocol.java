package linda.protocols;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ReadWriteProtocol {

    enum MODE {
        READING,
        WRITING
    }

    private int readers;
    private boolean writing;
    private MODE mode;
    private boolean readRequest;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition readCondition = lock.newCondition();
    private final Queue<Condition> writeConditions = new ArrayDeque<>();

    public ReadWriteProtocol() {
        readers = 0;
        mode = MODE.READING;
        readRequest = false;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public void requestWriting() {
        lock.lock();

        if(mode != MODE.WRITING) {
            if(readers > 0) {
                try {
                    Condition cond = lock.newCondition();
                    writeConditions.add(cond);
                    cond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else {
                mode = MODE.WRITING;
            }
        }

        assert mode == MODE.WRITING;
        writing = true;

        lock.unlock();
    }

    public void finishWriting() {
        assert writing;
        assert mode == MODE.WRITING;
        lock.lock();

        writing = false;
        if(readRequest) {
            mode = MODE.READING;
            readCondition.signalAll();
        }
        else if(writeConditions.peek() != null) {
            writeConditions.poll().signal();
        }

        lock.unlock();
    }

    public void requestReading() {
        lock.lock();

        if(mode != MODE.READING) {
            if(writing) {
                readRequest = true;
                try {
                    readCondition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else {
                mode = MODE.READING;
            }
        }

        assert mode == MODE.READING;
        readRequest = false;
        readers++;

        lock.unlock();
    }

    public void finishReading() {
        assert readers > 0;
        assert mode == MODE.READING;
        lock.lock();

        readers--;
        if(writeConditions.peek() != null && readers == 0) {
            mode = MODE.WRITING;
            writeConditions.poll().signal();
        }

        lock.unlock();
    }
}
