package linda.server;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.io.IOException;
import java.io.OutputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;

/** Client part of a client/server implementation of Linda.
 * It implements the Linda interface and propagates everything to the server it is connected to.
 * */
public class LindaClient implements Linda {
	
    /** Initializes the Linda implementation.
     *  @param serverURI the URI of the server, e.g. "rmi://localhost:4000/LindaServer" or "//localhost:4000/LindaServer".
     */
    private ServerInterface lc;
    public LindaClient(String serverURI) {
        try {
            lc = (ServerInterface) Naming.lookup(serverURI);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write(Tuple t) {
        try {
            lc.write(t);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Tuple take(Tuple template) {
        try {
             return lc.take(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public Tuple read(Tuple template) {
        try {
            return lc.read(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Tuple tryTake(Tuple template) {
        try {
            return lc.tryTake(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        try {
            return lc.tryRead(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        try {
            return lc.takeAll(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        try {
            return lc.readAll(template);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {


        try {
            RCallback rcallback= new RCallbackImpl(callback);
            UnicastRemoteObject.exportObject(rcallback, 0);
            lc.eventRegister(mode, timing, template, rcallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void debug(String prefix) {
        try {
            ROutputStream stream = new ROutputStream() {
                @Override
                public void close() throws RemoteException {
                    System.out.close();
                }

                @Override
                public void flush() throws RemoteException {
                    System.out.flush();
                }

                @Override
                public void write(byte[] b) throws RemoteException {
                    try {

                        System.out.write(b);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void write(byte[] b, int off, int len) throws RemoteException {
                    System.out.write(b, off, len);
                }

                @Override
                public void write(int b) throws RemoteException {
                    System.out.write(b);
                }
            };
            UnicastRemoteObject.exportObject(stream, 0);
            System.out.print(lc.debug(prefix));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // TO BE COMPLETED

}
