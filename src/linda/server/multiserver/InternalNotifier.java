package linda.server.multiserver;

public interface InternalNotifier {
    void notifyInvalidation(long uid);
}
