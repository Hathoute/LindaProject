package linda.utils;

import linda.server.LindaServer;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Helper {


    public static <T,R> Collection<R> collectionCast(Collection<T> ts) {
        return ts.stream()
                .map(t -> (R) t)
                .collect(Collectors.toList());
    }

    private static Random random = new Random();
    public static long getNextUniqueId() {
        //return random.nextLong();
        return (System.currentTimeMillis() << 20) | (System.nanoTime() & ~9223372036854251520L);
    }
}
