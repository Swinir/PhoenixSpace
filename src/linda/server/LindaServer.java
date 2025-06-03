package linda.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.io.Serializable;

import linda.Callback;
import linda.Linda;
import linda.Tuple;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;
import linda.shm.CentralizedLinda;

/**
 * Serveur Linda qui utilise CentralizedLinda en interne.
 */
public class LindaServer extends UnicastRemoteObject implements LindaRemote {
    
    private static final long serialVersionUID = 1L;
    private Linda linda;
    
    /**
     * Constructeur du serveur Linda.
     */
    public LindaServer() throws RemoteException {
        this.linda = new CentralizedLinda();
    }
    
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
        // Adaptateur pour transformer le RemoteCallback en Callback local
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
        // Même implémentation que eventRegister pour la compatibilité
        eventRegister(mode, timing, template, callback);
    }
    
    @Override
    public void debug(String prefix) throws RemoteException {
        linda.debug(prefix);
    }
    
    /**
     * Méthode principale pour démarrer le serveur.
     * @param args Arguments de la ligne de commande (service_name)
     */
    public static void main(String[] args) {
        try {
            // Vérifier les arguments
            if (args.length != 1) {
                System.err.println("Usage: java linda.server.LindaServer <service_name>");
                System.exit(1);
            }

            System.setProperty("java.rmi.server.hostname", "127.0.0.1");
            
            // Paramètres de connexion
            int port = 1099;
            String serviceName = args[0];
            
            // Créer le serveur
            LindaServer server = new LindaServer();
            
            // Créer ou récupérer le registre RMI
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(port);
                System.out.println("Registre RMI créé sur le port " + port);
            } catch (RemoteException e) {
                System.out.println("Registre RMI déjà existant sur le port " + port);
                registry = LocateRegistry.getRegistry(port);
            }

            // Enregistrer le serveur dans le registre
            registry.rebind(serviceName, server);
            
            System.out.println("Serveur Linda démarré sur: //localhost:" + port + "/" + serviceName);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du serveur: " + e);
            e.printStackTrace();
        }
    }
}