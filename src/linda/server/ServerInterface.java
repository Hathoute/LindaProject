package linda.server;
import linda.Linda;
import linda.Tuple;
import linda.server.cache.CacheInvalidator;
import linda.server.cache.TupleWrapper;

import java.rmi.*;
import java.util.Collection;

public interface ServerInterface extends Remote {

    public void subscribe(CacheInvalidator cache) throws RemoteException;

    public void write(Tuple t) throws RemoteException;

    public TupleWrapper take(Tuple template) throws RemoteException;

    public TupleWrapper read(Tuple template) throws RemoteException;

    public TupleWrapper tryTake(Tuple template) throws RemoteException;

    public TupleWrapper tryRead(Tuple template) throws RemoteException;

    public Collection<TupleWrapper> takeAll(Tuple template) throws RemoteException;

    public Collection<TupleWrapper> readAll(Tuple template) throws RemoteException;

    public enum eventMode { READ, TAKE };

    public enum eventTiming { IMMEDIATE, FUTURE };

    public void eventRegister(Linda.eventMode mode, Linda.eventTiming timing, Tuple template, RCallback rcallback) throws RemoteException;

    public void debug(String prefix) throws RemoteException;

}
