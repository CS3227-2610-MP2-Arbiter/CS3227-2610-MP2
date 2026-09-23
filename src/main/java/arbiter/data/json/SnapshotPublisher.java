package arbiter.data.json;

import java.io.IOException;
import java.nio.file.Path;

/** Replaces one committed snapshot; injectable for failure testing. */
@FunctionalInterface
interface SnapshotPublisher {
    void publish(Path target, byte[] bytes) throws IOException;
}
