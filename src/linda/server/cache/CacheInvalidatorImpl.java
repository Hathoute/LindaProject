package linda.server.cache;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class CacheInvalidatorImpl extends UnicastRemoteObject implements CacheInvalidator {

    protected CacheInvalidator ci;

    public CacheInvalidatorImpl(CacheInvalidator ci) throws RemoteException {
        this.ci = ci;
    }

    @Override
    public void invalidate(long uid) throws RemoteException {
        ci.invalidate(uid);
    }
}
