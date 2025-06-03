package linda.test;

import linda.Tuple;
import linda.shm.PersistentCentralizedLinda;
import linda.server.AutoSaveManager;

public class PersistenceTest {
    public static void main(String[] args) {
        try {
            // Créer une instance persistante
            PersistentCentralizedLinda linda = new PersistentCentralizedLinda("test_tuples.ser");

            // Optionnel: démarrer la sauvegarde automatique toutes les 30 secondes
            AutoSaveManager autoSave = new AutoSaveManager(linda, 30);
            autoSave.startAutoSave();

            // Ajouter quelques tuples
            linda.write(new Tuple("test", 1));
            linda.write(new Tuple("persistence", 42));
            linda.write(new Tuple(100, "data"));

            System.out.println("Tuples ajoutés");
            linda.debug("AVANT SAUVEGARDE");

            // Sauvegarder manuellement
            linda.saveTupleSpace();

            // Simuler un redémarrage en vidant l'espace
            linda.takeAll(new Tuple(Object.class, Object.class));
            linda.debug("APRÈS VIDAGE");

            // Restaurer depuis le fichier
            linda.loadTupleSpace();
            linda.debug("APRÈS RESTAURATION");

            // Arrêter la sauvegarde automatique
            autoSave.stopAutoSave();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
