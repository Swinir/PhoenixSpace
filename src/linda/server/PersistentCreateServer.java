package linda.server;

/**
 * Classe utilitaire pour lancer le serveur Linda persistant.
 */
public class PersistentCreateServer {

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java linda.server.CreatePersistentServer <service_name> [save_file]");
            System.err.println("  service_name : nom du service RMI");
            System.err.println("  save_file    : fichier de sauvegarde (optionnel, défaut: linda_tuples.ser)");
            return;
        }

        PersistentLindaServer.main(args);
    }
}
