package linda.test;

import linda.Linda;
import linda.Tuple;
import linda.server.LindaClient;
import linda.server.LindaServer;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiServerCycleTest {

    static class ServerData {
        public Integer port;
        public String url;
        public String context;

        public ServerData(String url, String context, Integer port) {
            this.url = url;
            this.context = context;
            this.port = port;
        }

        public ServerData(String url, String context) {
            this.url = url;
            this.context = context;
        }

        @Override
        public String toString() {
            return "//" + url + (port == null ? "" : (":" + port)) + "/" + context;
        }
    }

    public static List<ServerData> hierarchy = Arrays.asList(
            new ServerData("localhost", "Server1", 4000),
            new ServerData("localhost", "Server2", 4001),
            new ServerData("localhost", "Server3", 4002),
            new ServerData("localhost", "Server4", 4003)
            );

    public static void main(String[] a) {
        // Start servers
        List<LindaClient> clients = new ArrayList<>();
        List<LindaServer> servers = new ArrayList<>();
        try {
            for (ServerData sd : hierarchy) {
                LindaServer ls = new LindaServer();

                LocateRegistry.createRegistry(sd.port);
                Naming.rebind(sd.toString(), ls);
                System.out.println("LindaServer started at " + sd);

                clients.add(new LindaClient(sd.toString()));
                servers.add(ls);
            }

            for(int i = 0; i < servers.size() - 1; i++) {
                servers.get(i).addFallback(hierarchy.get(i+1).toString());
                servers.get(i+1).addFallback(hierarchy.get(i).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        final Linda mainClient = clients.get(0);

        new Thread() {
            public void run() {
                Tuple template = new Tuple(Integer.class, String.class);
                System.out.println("Attempting to read template " + template);
                Tuple t = mainClient.take(template);
                System.out.println("Found " + t);

                System.out.println("Attempting to take template " + template);
                t = mainClient.take(template);
                System.out.println("Found " + t);

                System.out.println("Attempting to take template " + template);
                t = mainClient.take(template);
                System.out.println("Found " + t);
            }
        }.start();

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println("Writing to 2nd fallback server");
                clients.get(3).write(new Tuple(10, "ttt"));
            }
        }.start();


    }
}
