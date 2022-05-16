package linda.server.cache;

import linda.Tuple;

import java.rmi.RemoteException;

public interface ClientCache {
    void cache(TupleWrapper t);
    Tuple tryRead(linda.Tuple template);
    void invalidate(long uid);
    CacheInvalidator getInvalidator();
}
