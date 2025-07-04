package linda.shm;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import linda.Tuple;
import linda.shm.CentralizedLinda;


/**
 * Extension de CentralizedLinda avec capacités de sauvegarde/restauration
 */
public class PersistentCentralizedLinda extends CentralizedLinda {

    private static final String DEFAULT_SAVE_FILE = "linda_tuples.ser";
    private final String saveFilePath;


    public PersistentCentralizedLinda() {
        this(DEFAULT_SAVE_FILE);
    }

    public PersistentCentralizedLinda(String saveFilePath) {
        super();
        this.saveFilePath = saveFilePath;
    }

    /**
     * Sauvegarde l'espace de tuples dans un fichier
     */
    public void saveTupleSpace() throws IOException {
        saveTupleSpace(this.saveFilePath);
    }

    /**
     * Sauvegarde l'espace de tuples dans le fichier spécifié
     */
    public void saveTupleSpace(String filename) throws IOException {
        lock.lock();
        try {
            // Créer une copie sérialisable de l'espace de tuples
            List<Tuple> tuplesToSave = new ArrayList<>();
            for (Tuple t : tupleSpace) {
                tuplesToSave.add(t.deepclone());
            }

            // Sauvegarder dans le fichier
            try (FileOutputStream fos = new FileOutputStream(filename);
                 ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(tuplesToSave);
                oos.flush();
            }

            System.out.println("Espace de tuples sauvegardé dans: " + filename);
            System.out.println("Nombre de tuples sauvegardés: " + tuplesToSave.size());

        } finally {
            lock.unlock();
        }
    }

    /**
     * Restaure l'espace de tuples depuis le fichier par défaut
     */
    public void loadTupleSpace() throws IOException, ClassNotFoundException {
        loadTupleSpace(this.saveFilePath);
    }

    /**
     * Restaure l'espace de tuples depuis le fichier spécifié
     */
    @SuppressWarnings("unchecked")
    public void loadTupleSpace(String filename) throws IOException, ClassNotFoundException {
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("Fichier de sauvegarde non trouvé: " + filename);
            return;
        }

        lock.lock();
        try {
            // Charger les tuples depuis le fichier
            List<Tuple> loadedTuples;
            try (FileInputStream fis = new FileInputStream(filename);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                loadedTuples = (List<Tuple>) ois.readObject();
            }

            // Vider l'espace actuel et restaurer les tuples
            tupleSpace.clear();
            for (Tuple t : loadedTuples) {
                tupleSpace.add(t.deepclone());
            }

            System.out.println("Espace de tuples restauré depuis: " + filename);
            System.out.println("Nombre de tuples restaurés: " + loadedTuples.size());

            // Signaler tous les threads en attente qu'il y a potentiellement de nouveaux tuples
            condition.signalAll();

        } finally {
            lock.unlock();
        }
    }

    /**
     * Sauvegarde automatique lors de l'arrêt
     */
    public void shutdown() {
        try {
            saveTupleSpace();
            System.out.println("Sauvegarde automatique effectuée lors de l'arrêt");
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde automatique: " + e.getMessage());
        }
    }
}
