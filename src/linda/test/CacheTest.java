package linda.test;

import linda.Linda;
import linda.Tuple;

public class CacheTest {

    private static int BATCH_SIZE = 10000;

    public static void main(String[] a) {
                
        //final Linda linda = new linda.shm.CentralizedLinda();
        final Linda linda = new linda.server.LindaClient("//localhost:4000/LindaServer");
        final Linda lindaNoCache = new linda.server.LindaClient("//localhost:4000/LindaServer", false);

        final Tuple template = new Tuple(String.class, Integer.class);

        // Cached thread
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                while(true) {
                    long start = System.currentTimeMillis();
                    Tuple t = null;
                    for(int i = 0; i < 100000; i++) {
                        t = linda.tryRead(template);
                    }
                    long end = System.currentTimeMillis();
                    System.out.println("Cached version avg per read: " + (float)(end-start)/100000 + "ms, last value was "
                            + (t == null ? "null" : t));
                }
            }
        }.start();

        // Non cached thread
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                while(true) {
                    long start = System.currentTimeMillis();
                    Tuple t = null;
                    for(int i = 0; i < 1000; i++) {
                        t = lindaNoCache.tryRead(template);
                    }
                    long end = System.currentTimeMillis();
                    System.out.println("Non cached version avg per read: " + (float)(end-start)/1000 + "ms, last value was "
                            + (t == null ? "null" : t));
                }
            }
        }.start();
                
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Tuple t1 = new Tuple("ss", 1);
                System.out.println("Writing: " + t1);
                lindaNoCache.write(t1);

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println("Taking tuple...");
                lindaNoCache.take(template);
            }
        }.start();
                
    }
}
