package linda.server;

import linda.Tuple;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RCallback extends Remote {
    void call(long eventId, Tuple t) throws RemoteException;

}
