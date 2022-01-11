package linda.server;

import linda.Callback;
import linda.Tuple;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RC_implementation implements RCallback{
    private Callback callback;
    public RC_implementation(Callback callback) throws RemoteException {
        this.callback=callback;
    }

    @Override
    public void call(Tuple t) throws RemoteException {
        callback.call(t);
    }
}
