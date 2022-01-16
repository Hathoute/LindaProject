package linda.server;
import linda.AsynchronousCallback;
import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.rmi.*;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

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
