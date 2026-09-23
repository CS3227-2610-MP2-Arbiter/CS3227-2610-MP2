package arbiter.model.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

class AnnotationTest {
    @Test
    void setLabelId_scaleValueAlreadySet_clearsScaleValue() {
        Annotation annotation = new Annotation();
        annotation.setScaleValue(4);

        annotation.setLabelId(12L);

        assertEquals(12L, annotation.getLabelId());
        assertNull(annotation.getScaleValue());
    }

    @Test
    void setScaleValue_labelIdAlreadySet_clearsLabelId() {
        Annotation annotation = new Annotation();
        annotation.setLabelId(12L);

        annotation.setScaleValue(4);

        assertEquals(4, annotation.getScaleValue());
        assertNull(annotation.getLabelId());
    }

    @Test
    void getLabelId_bothAnswerFieldsPopulated_exceptionThrown() throws ReflectiveOperationException {
        Annotation annotation = annotationWithBothAnswers();

        assertThrows(IllegalStateException.class, annotation::getLabelId);
    }

    @Test
    void getScaleValue_bothAnswerFieldsPopulated_exceptionThrown() throws ReflectiveOperationException {
        Annotation annotation = annotationWithBothAnswers();

        assertThrows(IllegalStateException.class, annotation::getScaleValue);
    }

    private Annotation annotationWithBothAnswers() throws ReflectiveOperationException {
        Annotation annotation = new Annotation();
        setField(annotation, "labelId", 12L);
        setField(annotation, "scaleValue", 4);
        return annotation;
    }

    private void setField(Annotation annotation, String name, Object value) throws ReflectiveOperationException {
        Field field = Annotation.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(annotation, value);
    }
}
