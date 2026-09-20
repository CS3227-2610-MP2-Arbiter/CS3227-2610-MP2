package arbiter.model.project;

/** The on-disk format the finished dataset is written in. Only the exporter writes these. */
public enum OutputFormat {
    CSV,
    JSON,
    COCO
}
