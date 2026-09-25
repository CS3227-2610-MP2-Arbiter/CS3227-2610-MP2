package arbiter.ui.shared;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.GeneralCodingRules;

import javafx.scene.Node;
import javafx.scene.control.Dialog;

/** Checks the shipped code follows the one UI and error convention (#8); see "Errors and logging". */
class UiConventionTest {
    private static JavaClasses shipped;

    @BeforeAll
    static void importShippedClasses() {
        shipped = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("arbiter");
    }

    @Test
    void dialogs_builtOnlyByDialogs() {
        noClasses().that().doNotHaveFullyQualifiedName(Dialogs.class.getName())
                .should().callConstructorWhere(calling("a dialog", call ->
                        call.getTargetOwner().isAssignableTo(Dialog.class)))
                .check(shipped);
    }

    @Test
    void logging_onlyThroughDiagnosticLog() {
        noClasses().that().doNotHaveFullyQualifiedName(DiagnosticLog.class.getName())
                .should().dependOnClassesThat().resideInAPackage("java.util.logging..")
                .orShould().dependOnClassesThat().belongToAnyOf(System.Logger.class, System.LoggerFinder.class)
                .check(shipped);
    }

    @Test
    void standardStreamsAndPrintStackTrace_neverUsed() {
        GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(shipped);
    }

    @Test
    void inlineStyles_neverSet() {
        noClasses().should().callMethodWhere(calling("Node.setStyle", call ->
                call.getName().equals("setStyle") && call.getTargetOwner().isAssignableTo(Node.class)))
                .check(shipped);
    }

    private static <T extends JavaCall<?>> DescribedPredicate<T> calling(String description,
            Predicate<T> test) {
        return DescribedPredicate.describe(description, test);
    }
}
