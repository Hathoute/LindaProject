package linda.server;

import linda.Callback;
import linda.Tuple;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RCallbackImpl implements RCallback{
    private Callback callback;
    public RCallbackImpl(Callback callback) throws RemoteException {
        this.callback=callback;
    }

    @Override
    public void call(long eventId, Tuple t) throws RemoteException {
        callback.call(eventId, t);
    }
}
