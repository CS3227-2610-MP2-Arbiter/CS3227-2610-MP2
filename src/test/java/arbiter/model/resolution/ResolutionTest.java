package arbiter.model.resolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

class ResolutionTest {
    @Test
    void setLabelId_scaleValueAlreadySet_clearsScaleValue() {
        Resolution resolution = new Resolution();
        resolution.setScaleValue(3.5);

        resolution.setLabelId(8L);

        assertEquals(8L, resolution.getLabelId());
        assertNull(resolution.getScaleValue());
    }

    @Test
    void setScaleValue_labelIdAlreadySet_clearsLabelId() {
        Resolution resolution = new Resolution();
        resolution.setLabelId(8L);

        resolution.setScaleValue(3.5);

        assertEquals(3.5, resolution.getScaleValue());
        assertNull(resolution.getLabelId());
    }

    @Test
    void getLabelId_bothResultFieldsPopulated_exceptionThrown() throws ReflectiveOperationException {
        Resolution resolution = resolutionWithBothResults();

        assertThrows(IllegalStateException.class, resolution::getLabelId);
    }

    @Test
    void getScaleValue_bothResultFieldsPopulated_exceptionThrown() throws ReflectiveOperationException {
        Resolution resolution = resolutionWithBothResults();

        assertThrows(IllegalStateException.class, resolution::getScaleValue);
    }

    private Resolution resolutionWithBothResults() throws ReflectiveOperationException {
        Resolution resolution = new Resolution();
        setField(resolution, "labelId", 8L);
        setField(resolution, "scaleValue", 3.5);
        return resolution;
    }

    private void setField(Resolution resolution, String name, Object value) throws ReflectiveOperationException {
        Field field = Resolution.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(resolution, value);
    }
}
