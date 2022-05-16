package linda.server.multiserver;

import linda.server.LindaClient;
import linda.server.cache.CacheInvalidator;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class InternalClient extends LindaClient {

    public InternalClient(String serverURI, InternalNotifier notifier) {
        // Use our own implementation of ClientCache
        super(serverURI, false);
        try {
            useCache = true;
            cache = new InternalMultiServerCache(notifier);
            lc.subscribe(cache.getInvalidator());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
