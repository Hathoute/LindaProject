package linda.server;

import java.io.OutputStream;
import java.io.PrintStream;
import java.rmi.Remote;
import java.rmi.RemoteException;

public class RPrintStream extends PrintStream implements Remote {

    public RPrintStream(OutputStream out) throws RemoteException {
        super(out);
    }


}
