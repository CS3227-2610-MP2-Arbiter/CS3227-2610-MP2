package arbiter.service;

/**
 * One label an annotator can choose for a SINGLE project (#14).
 *
 * @param id the label's identifier
 * @param key the label's short key
 * @param description what the label means, or null if it has none
 */
public record LabelOption(long id, String key, String description) {
}
