package arbiter.model.annotation;

/**
 * One labelled box drawn on a detection item, in image coordinates.
 *
 * <p>Coordinates are relative to the image, never to the screen, so they stay correct at any zoom
 * level.
 */
public class BoundingBox {
    /** Database identifier. */
    private Long id;

    /** Annotation this box belongs to. */
    private Long annotationId;

    /** Label applied to the box. */
    private Long labelId;

    /** Left edge in image coordinates. */
    private double x;

    /** Top edge in image coordinates. */
    private double y;

    /** Box width in image coordinates. */
    private double width;

    /** Box height in image coordinates. */
    private double height;

    /** Position in the box list, so order is stable. */
    private Integer sequence;

    /** Creates an empty BoundingBox. */
    public BoundingBox() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAnnotationId() {
        return annotationId;
    }

    public void setAnnotationId(Long annotationId) {
        this.annotationId = annotationId;
    }

    public Long getLabelId() {
        return labelId;
    }

    public void setLabelId(Long labelId) {
        this.labelId = labelId;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }
}
