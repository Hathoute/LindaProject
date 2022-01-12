package linda.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ROutputStream extends Remote {
    void close() throws RemoteException;
    void flush() throws RemoteException;
    void write(byte[] b) throws RemoteException;
    void write(byte[] b, int off, int len) throws RemoteException;
    void write(int b) throws RemoteException;
}
