package linda.server.cache;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCache extends Remote {
    void invalidate(long uid) throws RemoteException;
}
