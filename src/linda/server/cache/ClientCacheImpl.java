package linda.server.cache;

import linda.Tuple;
import linda.protocols.ReadWriteProtocol;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientCacheImpl implements ClientCache {
    private Map<Long, Tuple> cachedTuples;
    private ReadWriteProtocol protocol;

    public ClientCacheImpl() throws RemoteException {
        cachedTuples = new HashMap<>();
        protocol = new ReadWriteProtocol();
    }

    public void cache(TupleWrapper t) {
        protocol.requestWriting();
        cachedTuples.put(t.getUid(), t);
        protocol.finishWriting(true);
    }

    public Tuple tryRead(Tuple template) {
        protocol.requestReading();

        Tuple tp = null;
        for(Tuple t : cachedTuples.values()) {
            if(t.matches(template)) {
                tp = t;
                break;
            }
        }

        protocol.finishReading();
        return tp;
    }

    @Override
    public void invalidate(long uid) throws RemoteException {
        protocol.requestWriting();
        cachedTuples.remove(uid);
        protocol.finishWriting(true);
    }
}
