package linda.server;
import linda.AsynchronousCallback;
import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.server.cache.ClientCache;
import linda.server.cache.TupleWrapper;
import linda.shm.CentralizedLinda;
import linda.utils.Helper;

import java.rmi.*;
import java.rmi.server.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class LindaServer extends UnicastRemoteObject implements ServerInterface{
    private Linda myLinda;
    private long nextUid;
    private List<ClientCache> subscribedCaches;

    protected LindaServer() throws RemoteException {
        myLinda= new CentralizedLinda();
        subscribedCaches = new LinkedList<>();
        nextUid = 0;
    }

    @Override
    public void subscribe(ClientCache cache) throws RemoteException {
        subscribedCaches.add(cache);
    }

    private void notifyCaches(long uid) {
        List<ClientCache> disconnected = new LinkedList<>();

        for (ClientCache c : subscribedCaches) {
            try {
                c.invalidate(uid);
            } catch (RemoteException re) {
                disconnected.add(c);
            }
        }

        for (ClientCache c : disconnected) {
            subscribedCaches.remove(c);
        }
    }

    @Override
    public void write(Tuple t) throws RemoteException {
        myLinda.write(new TupleWrapper(t, ++nextUid));
    }

    @Override
    public TupleWrapper take(Tuple template) throws RemoteException {
       TupleWrapper t = (TupleWrapper) myLinda.take(template);
       notifyCaches(t.getUid());
       return t;
    }

    @Override
    public TupleWrapper read(Tuple template) throws RemoteException {
        return (TupleWrapper) myLinda.read(template);
    }

    @Override
    public TupleWrapper tryTake(Tuple template) throws RemoteException {
        TupleWrapper t = (TupleWrapper) myLinda.tryTake(template);
        if(t != null) {
            notifyCaches(t.getUid());
        }
        return t;
    }

    @Override
    public TupleWrapper tryRead(Tuple template) throws RemoteException {
        return (TupleWrapper) myLinda.tryRead(template);
    }

    @Override
    public Collection<TupleWrapper> takeAll(Tuple template) throws RemoteException {
        Collection<TupleWrapper> ts = Helper.collectionCast(myLinda.takeAll(template));

        ts.parallelStream().forEach(x -> notifyCaches(x.getUid()));

        return ts;
    }

    @Override
    public Collection<TupleWrapper> readAll(Tuple template) throws RemoteException {
        return Helper.collectionCast(myLinda.readAll(template));
    }

    @Override
    public void eventRegister(Linda.eventMode mode, Linda.eventTiming timing, Tuple template, RCallback rcallback) throws RemoteException {
        Callback callback = new Callback() {
            @Override
            public void call(Tuple t) {
                try {
                    if(mode == Linda.eventMode.TAKE) {
                        notifyCaches(((TupleWrapper) t).getUid());
                    }

                    rcallback.call(t);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };
        AsynchronousCallback cb = new AsynchronousCallback(callback);
        myLinda.eventRegister(mode,timing,template,cb);
    }

    @Override
    public void debug(String prefix) throws RemoteException {
        myLinda.debug(prefix);
    }

    /*
    @Override
    public String debug(String prefix) throws RemoteException {
        // Cette approche a été implémentée après avoir essayé de passer un RemoteOutputStream qui
        // contient une methode void write(int) et changer le System.out du serveur avec un PrintStream
        // issu du OutputStream du client, ce qui faisait que l'écriture du debug dans le client soit interrompue
        // par les écritures du client (ex. "SERVEUR\n" + "CLIENT\n" => "SERCLIENT\nVEUR\n"

        PrintStream serverStream = System.out;
        final boolean[] finished = new boolean[1];
        final ArrayList<Integer> writeBuffer = new ArrayList<>();
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                if(b == -1) {
                    finished[0] = true;
                    System.setOut(serverStream);
                    return;
                }

                writeBuffer.add(b);
            }
        };
        PrintStream printStream = new PrintStream(out);
        System.setOut(printStream);
        myLinda.debug(prefix);
        // Signal End of stream
        System.out.write(-1);

        // Wait for System.out.println() to process everything...
        while(!finished[0]) { }

        // Convert from int to byte
        byte[] bytes = new byte[writeBuffer.size()];
        for(int i = 0; i < writeBuffer.size(); i++) {
            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(writeBuffer.get(i));
            bytes[i] = b.get(3);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }*/
}
