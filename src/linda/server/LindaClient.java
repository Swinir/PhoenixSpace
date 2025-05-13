package linda.server;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.net.URI;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Client part of a client/server implementation of Linda.
 * It implements the Linda interface and propagates everything to the server it is connected to.
 * */
public class LindaClient implements Linda {
    
    private LindaRemote lindaRemote;
    // Garder une référence à tous les CallbackAdapter créés
    private Map<Callback, RemoteCallback> callbackAdapters;
    
    /** Initializes the Linda implementation.
     *  @param serverURI the URI of the server, e.g. "rmi://localhost:4000/LindaServer" or "//localhost:4000/LindaServer".
     */
    public LindaClient(String serverURI) {
        try {
            callbackAdapters = new HashMap<>();
            
            // Force Java RMI to use localhost for callbacks
            System.setProperty("java.rmi.server.hostname", "127.0.0.1");
            
            // Parse the URI
            URI uri;
            String host;
            int port;
            String serviceName;
            
            try {
                uri = new URI(serverURI);
                host = uri.getHost();
                port = uri.getPort();
                serviceName = uri.getPath().substring(1);
            } catch (Exception e) {
                // Alternative parsing for "//host:port/service" format
                String[] parts = serverURI.split("//");
                String[] hostPort = parts[1].split("/")[0].split(":");
                host = hostPort[0];
                port = Integer.parseInt(hostPort[1]);
                serviceName = parts[1].split("/")[1];
            }
            
            // If no port is specified, use 1099 by default
            if (port == -1) {
                port = 1099;
            }
            
            // Get the RMI registry
            Registry registry = LocateRegistry.getRegistry(host, port);
            
            // Get the Linda server reference
            lindaRemote = (LindaRemote) registry.lookup(serviceName);
            
            System.out.println("Connected to Linda server at: " + serverURI);
            
        } catch (Exception e) {
            System.err.println("Error connecting to server: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void write(Tuple t) {
        try {
            lindaRemote.write(t);
        } catch (RemoteException e) {
            System.err.println("Error during write call: " + e);
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Tuple take(Tuple template) {
        try {
            return lindaRemote.take(template);
        } catch (RemoteException e) {
            System.err.println("Error during take call: " + e);
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Tuple read(Tuple template) {
        try {
            return lindaRemote.read(template);
        } catch (RemoteException e) {
            System.err.println("Error during read call: " + e);
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Tuple tryTake(Tuple template) {
        try {
            return lindaRemote.tryTake(template);
        } catch (RemoteException e) {
            System.err.println("Error during tryTake call: " + e);
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Tuple tryRead(Tuple template) {
        try {
            return lindaRemote.tryRead(template);
        } catch (RemoteException e) {
            System.err.println("Error during tryRead call: " + e);
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        try {
            return lindaRemote.takeAll(template);
        } catch (RemoteException e) {
            System.err.println("Error during takeAll call: " + e);
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Collection<Tuple> readAll(Tuple template) {
        try {
            return lindaRemote.readAll(template);
        } catch (RemoteException e) {
            System.err.println("Error during readAll call: " + e);
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        try {
            // Use LocalCallback instead of RemoteCallback for event handling
            final Callback cb = callback;
            
            // Create a direct server-side callback handler
            lindaRemote.eventRegisterCallback(mode, timing, template, 
                new RemoteCallbackImpl(new Callback() {
                    @Override
                    public void call(Tuple t) {
                        cb.call(t);
                    }
                })
            );
            
        } catch (RemoteException e) {
            System.err.println("Error during eventRegister call: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void debug(String prefix) {
        try {
            lindaRemote.debug(prefix);
        } catch (RemoteException e) {
            System.err.println("Error during debug call: " + e);
            throw new RuntimeException(e);
        }
    }
}