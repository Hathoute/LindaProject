package linda.server.multiserver;

import linda.Tuple;
import linda.protocols.ReadWriteProtocol;
import linda.server.cache.ClientCache;
import linda.server.cache.ClientCacheImpl;
import linda.server.cache.TupleWrapper;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Observer;

public class InternalMultiServerCache extends ClientCacheImpl {

    protected InternalNotifier notifier;

    public InternalMultiServerCache(InternalNotifier notifier) throws RemoteException {
        super();
        this.notifier = notifier;
    }

    @Override
    public void invalidate(long uid) {
        super.invalidate(uid);
        this.notifier.notifyInvalidation(uid);
    }
}
