package linda.server;

/**
 * Classe utilitaire pour lancer le serveur Linda.
 */
public class CreateServer {
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java linda.server.CreateServer <service_name>");
            return;
        }
        
        LindaServer.main(args);
    }
}