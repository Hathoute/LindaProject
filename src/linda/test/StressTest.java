package linda.test;

import linda.Linda;
import linda.Tuple;

import java.util.Timer;

public class StressTest {

    private static int READS_PER_THREAD = 100000;
    private static int THREADS = 8;

    public static void main(String[] a) {
                
        //final Linda linda = new linda.shm.CentralizedLinda();
        final Linda linda = new linda.server.LindaClient("//localhost:4000/LindaServer", true);

        for(int t = 0; t < THREADS; t++) {
            int finalT = t;
            new Thread() {
                public void run() {
                    Tuple motif = new Tuple(Integer.class, String.class);
                    Tuple res = linda.read(motif);
                    // Count from first successful read
                    long startTime = System.currentTimeMillis();
                    for (int i = 0; i < READS_PER_THREAD; i++) {
                        res = linda.read(motif);
                    }
                    long endTime = System.currentTimeMillis();
                    System.out.println("Thread " + finalT + " finished in " + (endTime-startTime) + "ms, avg: " + (READS_PER_THREAD)/(endTime-startTime) + " reads/ms");
                }
            }.start();
        }
                
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Tuple t1 = new Tuple(4, "tst");
                System.out.println("(2) write: " + t1);
                linda.write(t1);

            }
        }.start();
                
    }
}
