package linda.server;
import java.net.InetAddress;
import java.rmi.*;
import java.rmi.registry.*;
public class Server {
    public static void main(String[] args) throws Exception {
        try{
            /* Pour la sécurité, facultatif dans l'exam
            System.setProperty("java.security.policy","client.policy");
            */
            LindaServer ls = new LindaServer();
            int port = 4000; // port par défault: 1099
            String url = "localhost";
            LocateRegistry.createRegistry(port);
            String URL = "//"+url+":"+
                    port+"/LindaServer";
            Naming.rebind(URL, ls);
            System.out.println("Le serveur est prêt");
        } catch (Exception e) {
            System.out.println("Erreur");
            System.out.println(e.toString());
        }
    }
}
