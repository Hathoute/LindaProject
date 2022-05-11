package linda.test;

import linda.Linda;
import linda.Tuple;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class StabilityTest {

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

        // Reading thread
        new Thread() {
            public void run() {
                Random random = new Random();
                long i = 0;
                while(true) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Tuple template = possibleTemplates.get(random.nextInt(possibleTemplates.size()));
                    if(random.nextBoolean()) {
                        linda.read(template);
                    }
                    else {
                        linda.take(template);
                    }
                    i++;

                    if(i % 10 == 0) {
                        System.out.println("Alive...");
                    }
                }
            }
        }.start();

        // Debugging thread
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                linda.debug("(Stability)");
            }
        }.start();


    }
}
