package linda.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;
import linda.Tuple;

/**
 * Interface distante pour le serveur Linda.
 */
public interface LindaRemote extends Remote {
    /**
     * Ajoute un tuple dans l'espace.
     */
    void write(Tuple t) throws RemoteException;
    
    /**
     * Retire et retourne un tuple correspondant au motif.
     */
    Tuple take(Tuple template) throws RemoteException;
    
    /**
     * Lit et retourne un tuple correspondant au motif sans le retirer.
     */
    Tuple read(Tuple template) throws RemoteException;
    
    /**
     * Version non bloquante de take.
     */
    Tuple tryTake(Tuple template) throws RemoteException;
    
    /**
     * Version non bloquante de read.
     */
    Tuple tryRead(Tuple template) throws RemoteException;
    
    /**
     * Retire et retourne tous les tuples correspondant au motif.
     */
    Collection<Tuple> takeAll(Tuple template) throws RemoteException;
    
    /**
     * Lit et retourne tous les tuples correspondant au motif sans les retirer.
     */
    Collection<Tuple> readAll(Tuple template) throws RemoteException;
    
    /**
     * Enregistre un callback pour être notifié lors de l'apparition d'un tuple.
     */
    void eventRegister(eventMode mode, eventTiming timing, Tuple template, RemoteCallback callback) throws RemoteException;
    
    /**
     * Nouvelle méthode pour enregistrement de callback avec implémentation directe.
     */
    void eventRegisterCallback(eventMode mode, eventTiming timing, Tuple template, RemoteCallback callback) throws RemoteException;
    
    /**
     * Affiche des informations de débogage.
     */
    void debug(String prefix) throws RemoteException;
}