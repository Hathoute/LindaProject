package linda.server;
import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;

import java.io.PrintStream;
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
        return myLinda.tryTake(template);
    }

    @Override
    public Tuple tryRead(Tuple template) throws RemoteException {
        return myLinda.tryRead(template);
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) throws RemoteException {
        return myLinda.takeAll(template);
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) throws RemoteException {
        return myLinda.readAll(template);
    }

    @Override
    public void eventRegister(Linda.eventMode mode, Linda.eventTiming timing, Tuple template, RCallback rcallback) throws RemoteException {
            Callback callback = new Callback() {
                @Override
                public void call(Tuple t) {
                    try {
                        rcallback.call(t);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            };
            myLinda.eventRegister(mode,timing,template,callback);
    }

    @Override
    public void debug(String prefixe, RPrintStream stream) throws RemoteException {
        PrintStream serverStream = System.out;
        System.setOut(stream);
        myLinda.debug(prefixe);
        System.setOut(serverStream);
    }
}
