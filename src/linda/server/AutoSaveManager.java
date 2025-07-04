package linda.server;

import linda.shm.PersistentCentralizedLinda;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gestionnaire de sauvegarde périodique
 */
public class AutoSaveManager {
    private final PersistentCentralizedLinda linda;
    private final ScheduledExecutorService scheduler;
    private final int saveIntervalSeconds;

    public AutoSaveManager(PersistentCentralizedLinda linda, int saveIntervalSeconds) {
        this.linda = linda;
        this.saveIntervalSeconds = saveIntervalSeconds;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Démarre la sauvegarde automatique périodique
     */
    public void startAutoSave() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                linda.saveTupleSpace();
                System.out.println("Sauvegarde automatique effectuée");
            } catch (Exception e) {
                System.err.println("Erreur lors de la sauvegarde automatique: " + e.getMessage());
            }
        }, saveIntervalSeconds, saveIntervalSeconds, TimeUnit.SECONDS);

        System.out.println("Sauvegarde automatique démarrée (intervalle: " + saveIntervalSeconds + "s)");
    }

    /**
     * Arrête la sauvegarde automatique
     */
    public void stopAutoSave() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        System.out.println("Sauvegarde automatique arrêtée");
    }
}
