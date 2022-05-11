package linda.server.cache;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import linda.Tuple;

import java.io.Serializable;
import java.util.stream.Collectors;

public class TupleWrapper extends Tuple {
    private final long uid;

    public TupleWrapper(Tuple t, long uid) {
        super(t.toArray(new Serializable[0]));
        this.uid = uid;
    }

    public long getUid() {
        return uid;
    }
}
