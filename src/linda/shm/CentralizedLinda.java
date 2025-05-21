package linda.shm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

/** Implémentation centralisée de Linda en mémoire partagée. */
public class CentralizedLinda implements Linda {

    private final List<Tuple> tupleSpace;
    private final Lock lock;
    private final Condition condition;
    private final List<CallbackRegistration> callbacks;

    public CentralizedLinda() {
        this.tupleSpace = new ArrayList<>();
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
        this.callbacks = new ArrayList<>();
    }

    @Override
    public void write(Tuple t) {
        lock.lock();
        try {
            // Clone the tuple before any operations
            Tuple tupleToWrite = t.deepclone();
            
            // Vérification des callbacks avant d'ajouter le tuple
            List<CallbackRegistration> triggeredCallbacks = new ArrayList<>();
            for (CallbackRegistration registration : callbacks) {
                if (tupleToWrite.matches(registration.template)) {
                    triggeredCallbacks.add(registration);
                }
            }

            // Add the tuple to the space first
            tupleSpace.add(tupleToWrite.deepclone());
            
            // Process callbacks in the order they were registered
            // First process READ callbacks, then TAKE callbacks
            List<CallbackRegistration> readCallbacks = new ArrayList<>();
            List<CallbackRegistration> takeCallbacks = new ArrayList<>();
            
            for (CallbackRegistration registration : triggeredCallbacks) {
                if (registration.mode == eventMode.READ) {
                    readCallbacks.add(registration);
                } else {
                    takeCallbacks.add(registration);
                }
                // Remove from the callbacks list
                callbacks.remove(registration);
            }
            
            // Process READ callbacks first
            for (CallbackRegistration registration : readCallbacks) {
                try {
                    registration.callback.call(tupleToWrite.deepclone());
                } catch (Exception e) {
                    System.err.println("Error in callback: " + e);
                }
            }
            
            // Process TAKE callbacks - only execute one TAKE callback
            if (!takeCallbacks.isEmpty()) {
                CallbackRegistration takeReg = takeCallbacks.get(0);
                
                // Remove the tuple from space
                Tuple matchingTuple = findMatchingTuple(takeReg.template);
                if (matchingTuple != null) {
                    tupleSpace.remove(matchingTuple);
                    
                    try {
                        takeReg.callback.call(tupleToWrite.deepclone());
                    } catch (Exception e) {
                        System.err.println("Error in callback: " + e);
                    }
                }
                
                // Re-register the remaining TAKE callbacks that couldn't be executed
                for (int i = 1; i < takeCallbacks.size(); i++) {
                    callbacks.add(takeCallbacks.get(i));
                }
            }

            // Signal aux threads en attente
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Tuple take(Tuple template) {
        lock.lock();
        try {
            Tuple result = null;
            while ((result = findMatchingTuple(template)) == null) {
                try {
                    condition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            tupleSpace.remove(result);
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Tuple read(Tuple template) {
        lock.lock();
        try {
            Tuple result = null;
            while ((result = findMatchingTuple(template)) == null) {
                try {
                    condition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            return result.deepclone();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Tuple tryTake(Tuple template) {
        lock.lock();
        try {
            Tuple result = findMatchingTuple(template);
            if (result != null) {
                tupleSpace.remove(result);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Tuple tryRead(Tuple template) {
        lock.lock();
        try {
            Tuple result = findMatchingTuple(template);
            return result != null ? result.deepclone() : null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        lock.lock();
        try {
            Collection<Tuple> results = new ArrayList<>();
            for (Tuple t : tupleSpace) {
                if (t.matches(template)) {
                    results.add(t);
                }
            }
            tupleSpace.removeAll(results);
            return results;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        lock.lock();
        try {
            Collection<Tuple> results = new ArrayList<>();
            for (Tuple t : tupleSpace) {
                if (t.matches(template)) {
                    results.add(t);
                }
            }
            return results;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        lock.lock();
        try {
            // Vérification de déclenchement immédiat
            if (timing == eventTiming.IMMEDIATE) {
                Tuple match = findMatchingTuple(template);
                if (match != null) {
                    if (mode == eventMode.TAKE) {
                        tupleSpace.remove(match);
                    }
                    try {
                        callback.call(match.deepclone());
                    } catch (Exception e) {
                        System.err.println("Error in callback: " + e);
                    }
                    return;
                }
            }

            // Enregistrement du callback pour les futurs tuples
            callbacks.add(new CallbackRegistration(mode, timing, template.deepclone(), callback));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void debug(String prefix) {
        lock.lock();
        try {
            System.out.println(prefix + " Tuples in space: " + tupleSpace);
            System.out.println(prefix + " Registered callbacks: " + callbacks.size());
        } finally {
            lock.unlock();
        }
    }

    private Tuple findMatchingTuple(Tuple template) {
        for (Tuple t : tupleSpace) {
            if (t.matches(template)) {
                return t;
            }
        }
        return null;
    }

    private static class CallbackRegistration {
        final eventMode mode;
        final eventTiming timing;
        final Tuple template;
        final Callback callback;

        public CallbackRegistration(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
            this.mode = mode;
            this.timing = timing;
            this.template = template;
            this.callback = callback;
        }
    }
}
