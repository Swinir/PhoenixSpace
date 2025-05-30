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
    private final List<CallbackRegistration> callbacks;
    private final Lock lock;
    private final Condition condition;

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
            // On clone le tuple pour éviter les modifications externes
            Tuple tupleToWrite = t.deepclone();

            // Ajout du tuple dans l'espace de tuples
            tupleSpace.add(tupleToWrite.deepclone());

            List<CallbackRegistration> matchingCallbacks = new ArrayList<>();
            for (CallbackRegistration registration : new ArrayList<>(callbacks)) {
                if (tupleToWrite.matches(registration.template)) {
                    matchingCallbacks.add(registration);
                }
            }

            for (CallbackRegistration registration : matchingCallbacks) {
                if (registration.mode == eventMode.READ) {
                    callbacks.remove(registration);
                    try {
                        registration.callback.call(tupleToWrite.deepclone());
                    } catch (Exception e) {
                        System.err.println("Error in callback: " + e);
                    }
                }
            }

            CallbackRegistration takeCallback = null;
            for (CallbackRegistration registration : matchingCallbacks) {
                if (registration.mode == eventMode.TAKE) {
                    takeCallback = registration;
                    break;
                }
            }
            if (takeCallback != null) {
                callbacks.remove(takeCallback);
                Tuple matchingTuple = findMatchingTuple(takeCallback.template);
                if (matchingTuple != null) {
                    tupleSpace.remove(matchingTuple);
                    try {
                        takeCallback.callback.call(matchingTuple.deepclone());
                    } catch (Exception e) {
                        System.err.println("Error in callback: " + e);
                    }
                }
            }

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
    public Collection readAll(Tuple template) {
        lock.lock();
        try {
            Collection results = new ArrayList<>();
            for (Tuple t : tupleSpace) {  // Utilisation de tupleSpace pour éviter les modifications concurrentes
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
            // Vérification de la validité des paramètres
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
