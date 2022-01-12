package linda.server;
import linda.AsynchronousCallback;
import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.rmi.*;
import java.util.Collection;

public interface ServerInterface extends Remote {
    public void write(Tuple t) throws RemoteException;
    public Tuple take(Tuple template) throws RemoteException;

    public Tuple read(Tuple template) throws RemoteException;

    public Tuple tryTake(Tuple template) throws RemoteException;

    public Tuple tryRead(Tuple template) throws RemoteException;

    public Collection<Tuple> takeAll(Tuple template) throws RemoteException;

    public Collection<Tuple> readAll(Tuple template) throws RemoteException;

    public enum eventMode { READ, TAKE };

    public enum eventTiming { IMMEDIATE, FUTURE };

    public void eventRegister(Linda.eventMode mode, Linda.eventTiming timing, Tuple template, RCallback rcallback) throws RemoteException;

    public String debug(String prefixe) throws RemoteException;

}
