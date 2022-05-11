package linda.server;

import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.server.cache.ClientCacheImpl;
import linda.server.cache.TupleWrapper;
import linda.utils.Helper;

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
    private ClientCacheImpl cache;
    private final boolean useCache;

    public LindaClient(String serverURI, boolean useCache) {
        this.useCache = useCache;
        try {
            lc = (ServerInterface) Naming.lookup(serverURI);
            if(useCache) {
                cache = new ClientCacheImpl();
                UnicastRemoteObject.exportObject(cache, 0);
                lc.subscribe(cache);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LindaClient(String serverURI) {
        this(serverURI, true);
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
            if(useCache) {
                Tuple t = cache.tryRead(template);
                if (t != null) {
                    return t;
                }
            }

            TupleWrapper tw = lc.read(template);
            if(useCache) {
                cache.cache(tw);
            }
            return tw;
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
            if(useCache) {
                Tuple t = cache.tryRead(template);
                if (t != null) {
                    return t;
                }
            }

            TupleWrapper tw = lc.tryRead(template);
            if(useCache && tw != null) {
                cache.cache(tw);
            }
            return tw;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        try {
            return Helper.collectionCast(lc.takeAll(template));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        try {
            Collection<TupleWrapper> tw = lc.readAll(template);
            if(useCache) {
                tw.parallelStream().forEach(x -> cache.cache(x));
            }
            return Helper.collectionCast(tw);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        if(useCache && mode == eventMode.READ && timing == eventTiming.IMMEDIATE) {
            Tuple t = cache.tryRead(template);
            if(t != null) {
                callback.call(t);
                return;
            }
        }

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
            lc.debug(prefix);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /*@Override
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
    }*/

    // TO BE COMPLETED

}
