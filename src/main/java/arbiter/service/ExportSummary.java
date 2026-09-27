package arbiter.service;

import java.nio.file.Path;

/**
 * A project's export (#37) as the adjudicator previews it, and then as it was written.
 *
 * @param file the export's full path under the workspace's {@code exports/} folder
 * @param resolvedCount the project's items that have a {@code Resolution} record
 * @param unresolvedCount the project's items that have none, which the export marks {@code UNRESOLVED}
 */
public record ExportSummary(Path file, long resolvedCount, long unresolvedCount) {
}
