package linda.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import linda.Tuple;

/**
 * Interface distante pour les callbacks.
 */
public interface RemoteCallback extends Remote {
    
    /**
     * Méthode appelée quand un tuple correspondant au motif est trouvé.
     */
    void call(Tuple t) throws RemoteException;
}