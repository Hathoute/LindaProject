package linda.utils;

import linda.server.LindaServer;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Helper {


    public static <T,R> Collection<R> collectionCast(Collection<T> ts) {
        return ts.stream()
                .map(t -> (R) t)
                .collect(Collectors.toList());
    }
}
