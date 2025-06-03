package linda.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

import linda.Callback;
import linda.Tuple;

/**
 * Adaptateur pour transformer un Callback local en RemoteCallback.
 */
public class CallbackAdapter extends UnicastRemoteObject implements RemoteCallback {
    
    private static final long serialVersionUID = 1L;
    private Callback callback;
    
    /**
     * Constructeur qui utilise les paramètres par défaut.
     */
    public CallbackAdapter(Callback callback) throws RemoteException {
        super(0); // Utilise un port aléatoire
        this.callback = callback;
    }
    
    /**
     * Constructeur qui spécifie le port sur lequel le callback sera exposé.
     */
    public CallbackAdapter(Callback callback, int port) throws RemoteException {
        super(port);
        this.callback = callback;
    }
    
    /**
     * Constructeur complet permettant de spécifier toutes les options de communication RMI.
     */
    public CallbackAdapter(Callback callback, int port, 
                          RMIClientSocketFactory csf,
                          RMIServerSocketFactory ssf) throws RemoteException {
        super(port, csf, ssf);
        this.callback = callback;
    }
    
    @Override
    public void call(Tuple t) throws RemoteException {
        callback.call(t);
    }
}