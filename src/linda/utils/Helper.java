package linda.utils;

import java.util.Collection;
import java.util.stream.Collectors;

public class Helper {


    public static <T,R> Collection<R> collectionCast(Collection<T> ts) {
        return ts.stream()
                .map(t -> (R) t)
                .collect(Collectors.toList());
    }
}
