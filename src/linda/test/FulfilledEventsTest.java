package linda.test;

import linda.AsynchronousCallback;
import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FulfilledEventsTest {

    private static List<Tuple> possibleTuples;
    private static List<Tuple> possibleTemplates;

    static {
        possibleTuples = new ArrayList<>();
        possibleTuples.add(new Tuple(1, "meme"));
        possibleTuples.add(new Tuple(3, 4));
        possibleTuples.add(new Tuple("tst"));
        possibleTuples.add(new Tuple(22, 11));
        possibleTuples.add(new Tuple("meme", 22));

        possibleTemplates = new ArrayList<>();
        possibleTemplates.add(new Tuple(Integer.class, String.class));
        possibleTemplates.add(new Tuple(String.class, Integer.class));
        possibleTemplates.add(new Tuple(Integer.class, Integer.class));
        possibleTemplates.add(new Tuple(String.class));
    }

    public static void main(String[] a) {
                
        //final Linda linda = new linda.shm.CentralizedLinda();
        final Linda linda = new linda.server.LindaClient("//localhost:4000/LindaServer");

        // Writing thread
        new Thread() {
            public void run() {
                Random random = new Random();
                while(true) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Tuple t = possibleTuples.get(random.nextInt(possibleTuples.size()));
                    linda.write(t);
                }
            }
        }.start();

        // Subscriber thread
        final IntegerWrapper total = new IntegerWrapper();
        final IntegerWrapper fulfilled = new IntegerWrapper();
        new Thread() {
            public void run() {
                Random random = new Random();
                Lock lock = new ReentrantLock();

                while(true) {
                    Tuple template = possibleTemplates.get(random.nextInt(possibleTemplates.size()));
                    Callback callback = new Callback() {
                        @Override
                        public void call(long eventId, Tuple t) {
                            lock.lock();
                            fulfilled.value++;
                            lock.unlock();
                        }
                    };
                    linda.eventRegister(
                            random.nextBoolean() ? Linda.eventMode.READ : Linda.eventMode.TAKE,
                            random.nextBoolean() ? Linda.eventTiming.IMMEDIATE : Linda.eventTiming.FUTURE,
                            template,
                            random.nextBoolean() ? new AsynchronousCallback(callback) : callback);
                    total.value++;
                }
            }
        }.start();

        // Debugging thread
        new Thread() {
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    System.out.println("Fulfilled " + fulfilled.value + "/" + total.value);
                }
            }
        }.start();


    }
}

class IntegerWrapper {
    public int value = 0;
}
