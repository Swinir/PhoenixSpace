package linda.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;
import linda.shm.PersistentCentralizedLinda;

/**
 * Serveur Linda avec capacités de persistance
 */
public class PersistentLindaServer extends UnicastRemoteObject implements LindaRemote {
    private static final long serialVersionUID = 1L;
    private PersistentCentralizedLinda linda;

    public PersistentLindaServer() throws RemoteException {
        this("linda_tuples.ser");
    }

    public PersistentLindaServer(String saveFile) throws RemoteException {
        this.linda = new PersistentCentralizedLinda(saveFile);

        // Tentative de restauration au démarrage
        try {
            linda.loadTupleSpace();
        } catch (Exception e) {
            System.out.println("Aucune sauvegarde trouvée ou erreur de chargement: " + e.getMessage());
        }

        // Hook d'arrêt pour sauvegarde automatique
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            linda.shutdown();
        }));
    }

    // Délégation de toutes les méthodes Linda
    @Override
    public void write(Tuple t) throws RemoteException {
        linda.write(t);
    }

    @Override
    public Tuple take(Tuple template) throws RemoteException {
        return linda.take(template);
    }

    @Override
    public Tuple read(Tuple template) throws RemoteException {
        return linda.read(template);
    }

    @Override
    public Tuple tryTake(Tuple template) throws RemoteException {
        return linda.tryTake(template);
    }

    @Override
    public Tuple tryRead(Tuple template) throws RemoteException {
        return linda.tryRead(template);
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) throws RemoteException {
        return linda.takeAll(template);
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) throws RemoteException {
        return linda.readAll(template);
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, RemoteCallback callback) throws RemoteException {
        linda.eventRegister(mode, timing, template, new Callback() {
            @Override
            public void call(Tuple t) {
                try {
                    callback.call(t);
                } catch (RemoteException e) {
                    System.err.println("Erreur lors de l'appel du callback distant: " + e);
                }
            }
        });
    }

    @Override
    public void eventRegisterCallback(eventMode mode, eventTiming timing, Tuple template, RemoteCallback callback) throws RemoteException {
        eventRegister(mode, timing, template, callback);
    }

    @Override
    public void debug(String prefix) throws RemoteException {
        linda.debug(prefix);
    }

    /**
     * Méthode pour forcer une sauvegarde manuelle
     */
    public void forceSave() throws RemoteException {
        try {
            linda.saveTupleSpace();
        } catch (Exception e) {
            throw new RemoteException("Erreur lors de la sauvegarde", e);
        }
    }

    /**
     * Méthode principale pour démarrer le serveur persistant
     */
    public static void main(String[] args) {
        try {
            if (args.length < 1 || args.length > 2) {
                System.err.println("Usage: java linda.server.PersistentLindaServer <service_name> [save_file]");
                System.exit(1);
            }

            System.setProperty("java.rmi.server.hostname", "127.0.0.1");

            int port = 1099;
            String serviceName = args[0];
            String saveFile = args.length > 1 ? args[1] : "linda_tuples.ser";

            PersistentLindaServer server = new PersistentLindaServer(saveFile);

            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(port);
                System.out.println("Registre RMI créé sur le port " + port);
            } catch (RemoteException e) {
                System.out.println("Registre RMI déjà existant sur le port " + port);
                registry = LocateRegistry.getRegistry(port);
            }

            registry.rebind(serviceName, server);
            System.out.println("Serveur Linda persistant démarré sur: //localhost:" + port + "/" + serviceName);
            System.out.println("Fichier de sauvegarde: " + saveFile);

        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du serveur persistant: " + e);
            e.printStackTrace();
        }
    }
}
