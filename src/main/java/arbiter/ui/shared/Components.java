package arbiter.ui.shared;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/** Builds the basic display pieces both roles' screens share; their look comes from {@link Styles}. */
public final class Components {
    private Components() {
    }

    /** Returns a screen or form heading. */
    public static Label pageTitle(String text) {
        return styled(wrapping(text), Styles.PAGE_TITLE);
    }

    /** Returns plain text, such as a message or data. */
    public static Label text(String text) {
        return wrapping(text);
    }

    /** Returns secondary text that explains a control or a rule. */
    public static Label hint(String text) {
        return styled(wrapping(text), Styles.HINT);
    }

    /**
     * Returns an empty inline message for input the user can correct on the same form. Set its text
     * with {@link ErrorMessages#of}; failed actions use {@link Dialogs#showError} instead.
     */
    public static Label errorText() {
        return styled(wrapping(""), Styles.ERROR_TEXT);
    }

    /** Returns the content of a screen that has nothing to show yet. */
    public static VBox emptyState(String title, String message) {
        return styled(new VBox(pageTitle(title), hint(message)), Styles.EMPTY_STATE);
    }

    /** Returns a screen's content of these controls, laid out from the top. */
    public static VBox page(Node... controls) {
        return styled(new VBox(controls), Styles.PAGE);
    }

    /** Returns a screen's content of these controls, laid out from the top, which scrolls if it is taller. */
    public static ScrollPane scrollingPage(Node... controls) {
        ScrollPane scroll = styled(new ScrollPane(page(controls)), Styles.FORM_SCROLL);
        scroll.setFitToWidth(true);
        return scroll;
    }

    /** Returns a card of these controls, one item in a list of cards. */
    public static VBox card(Node... controls) {
        return styled(new VBox(controls), Styles.CARD);
    }

    /** Returns a form of these controls, centred in the available space, which scrolls if it is taller. */
    public static ScrollPane form(Node... controls) {
        VBox form = styled(new VBox(controls), Styles.FORM);
        form.setAlignment(Pos.CENTER_LEFT);
        StackPane centred = new StackPane(form);
        ScrollPane scroll = styled(new ScrollPane(centred), Styles.FORM_SCROLL);
        scroll.setFitToWidth(true);
        // Filling at least the visible height centres a short form. Fitting it to that height instead would
        // squeeze a tall one, cutting off its wrapped text, and could hide the scroll bar it needs.
        centred.minHeightProperty().bind(Bindings.createDoubleBinding(() -> scroll.getViewportBounds().getHeight(),
                scroll.viewportBoundsProperty()));
        return scroll;
    }

    /** Returns a form's buttons: {@code confirm}, and Cancel, which runs {@code cancel} and answers Escape. */
    public static ButtonBar formActions(Button confirm, Runnable cancel) {
        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(event -> cancel.run());
        ButtonBar actions = new ButtonBar();
        ButtonBar.setButtonData(confirm, ButtonBar.ButtonData.OK_DONE);
        ButtonBar.setButtonData(cancelButton, ButtonBar.ButtonData.CANCEL_CLOSE);
        actions.getButtons().setAll(confirm, cancelButton);
        return actions;
    }

    /** Returns a table column titled {@code title} that shows {@code value} of each row. */
    public static <T, V> TableColumn<T, V> column(String title, Function<T, V> value) {
        TableColumn<T, V> column = new TableColumn<>(title);
        column.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(value.apply(row.getValue())));
        return column;
    }

    /** Returns a {@link #column} whose text wraps onto more lines, for text too long to fit its width. */
    public static <T> TableColumn<T, String> wrappingColumn(String title, Function<T, String> value) {
        TableColumn<T, String> column = column(title, value);
        column.setCellFactory(ignored -> {
            TableCell<T, String> cell = new TableCell<>();
            Text text = new Text();
            text.textProperty().bind(cell.itemProperty());
            text.fillProperty().bind(cell.textFillProperty());
            // A row is only as tall as its cells' single-line text, so the text wraps to the column itself.
            text.wrappingWidthProperty().bind(Bindings.createDoubleBinding(() -> column.getWidth()
                    - cell.getInsets().getLeft() - cell.getInsets().getRight(), column.widthProperty(),
                    cell.insetsProperty()));
            cell.setGraphic(text);
            return cell;
        });
        return column;
    }

    /**
     * Returns a table column with one {@code text} button per row, which runs {@code action} on that row
     * and is disabled for rows that {@code disabled} matches.
     */
    public static <T> TableColumn<T, T> buttonColumn(String text, Predicate<T> disabled, Consumer<T> action) {
        TableColumn<T, T> column = new TableColumn<>();
        column.setSortable(false);
        column.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(T row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                Button button = new Button(text);
                button.setDisable(disabled.test(row));
                button.setOnAction(event -> action.accept(row));
                setGraphic(button);
            }
        });
        return column;
    }

    private static Label wrapping(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        // Otherwise a layout short of room, such as a page with a table, squeezes wrapped lines off.
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    private static <T extends Node> T styled(T node, String styleClass) {
        node.getStyleClass().add(styleClass);
        return node;
    }
}
