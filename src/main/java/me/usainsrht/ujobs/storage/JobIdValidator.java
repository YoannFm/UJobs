package me.usainsrht.ujobs.storage;

import org.bukkit.NamespacedKey;

/** Which job ids the PDC storage can keep. */
public final class JobIdValidator {

    private static final String NAMESPACE = "ujobs";

    private JobIdValidator() {
    }

    /**
     * The PDC storage keeps each job under a {@link NamespacedKey}, which only allows lowercase letters, digits
     * and {@code _ - . /}. A job whose id is not a valid key cannot be saved there, and the storage skips it.
     */
    public static boolean isStorableInPdc(String jobId) {
        if (jobId.isEmpty()) return false;
        NamespacedKey key = NamespacedKey.fromString(NAMESPACE + ":" + jobId);
        return key != null && key.getKey().equals(jobId);
    }
}
