package linda.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import linda.Callback;
import linda.Tuple;

/**
 * Implémentation du callback distant avec gestion améliorée des erreurs réseau.
 */
public class RemoteCallbackImpl extends UnicastRemoteObject implements RemoteCallback {
    
    private static final long serialVersionUID = 1L;
    private final Callback callback;
    
    public RemoteCallbackImpl(Callback callback) throws RemoteException {
        // Utiliser le port 0 pour que RMI choisisse un port disponible automatiquement
        super(0);
        this.callback = callback;
    }
    
    @Override
    public void call(Tuple t) throws RemoteException {
        try {
            callback.call(t);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'exécution du callback: " + e);
            e.printStackTrace();
            throw new RemoteException("Erreur d'exécution du callback", e);
        }
    }
}