package linda.server;
import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;

import java.rmi.*;
import java.rmi.server.*;
import java.util.Collection;

public class LindaServer extends UnicastRemoteObject implements ServerInterface{
    private Linda myLinda;

    protected LindaServer() throws RemoteException {
        myLinda= new CentralizedLinda();
    }

    @Override
    public void write(Tuple t) throws RemoteException {
            myLinda.write(t);
    }

    @Override
    public Tuple take(Tuple template) throws RemoteException {
       return myLinda.take(template);
    }

    @Override
    public Tuple read(Tuple template) throws RemoteException {
        return myLinda.read(template);
    }

    @Override
    public Tuple tryTake(Tuple template) throws RemoteException {
        return null;
    }

    @Override
    public Tuple tryRead(Tuple template) throws RemoteException {
        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) throws RemoteException {
        return null;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) throws RemoteException {
        return null;
    }

    @Override
    public void eventRegister(Linda.eventMode mode, Linda.eventTiming timing, Tuple template, Callback callback) throws RemoteException {


    }
}
