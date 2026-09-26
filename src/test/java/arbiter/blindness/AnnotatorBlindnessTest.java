package arbiter.blindness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import arbiter.blindness.BlindnessRule.Hidden;
import arbiter.blindness.BlindnessRule.Violation;
import arbiter.blindness.fixture.ui.adjudicator.DisputeScreen;
import arbiter.blindness.fixture.ui.annotator.AdjudicatorLinkScreen;
import arbiter.blindness.fixture.ui.annotator.BlindScreen;
import arbiter.blindness.fixture.ui.annotator.InterfaceScreen;
import arbiter.blindness.fixture.ui.annotator.ReferenceScreen;
import arbiter.blindness.fixture.ui.annotator.ResolvedLabelScreen;
import arbiter.blindness.fixture.ui.annotator.TaskScreen;
import arbiter.blindness.fixture.ui.annotator.TeamProgressScreen;
import arbiter.blindness.fixture.ui.annotator.TrustedResultScreen;
import arbiter.blindness.fixture.ui.annotator.UserDataScreen;
import arbiter.blindness.fixture.ui.queue.QueueScreen;
import arbiter.blindness.fixture.ui.shared.ResolvedViewModel;

/**
 * Fails if annotator-facing code can reach another annotator's answers, a resolved result or
 * cross-annotator progress (rule 1); see "How blindness is enforced" in the developer guide.
 *
 * <p>The rule runs over the shipped classes, and over fixtures that each leak one way, so the build
 * fails if it stops catching a leak before the annotator's screens exist to catch it in.
 */
class AnnotatorBlindnessTest {
    /**
     * The service reads annotator-facing code may call without the walk entering them. Each one's
     * scoping to the signed-in annotator is tested by its own feature, so adding one is a reviewed
     * decision: annotator reads go through {@code AnnotationService.forCurrentUser} (architecture), and
     * submission (#17) goes through {@code AnnotationService.submit}, which reads the annotator's own answers to
     * place the queue and returns only their own queue.
     */
    private static final Set<String> TRUSTED_ENTRY_POINTS = Set.of("arbiter.service.AnnotationService.forCurrentUser",
            "arbiter.service.AnnotationService.submit");

    private static final BlindnessRule SHIPPED = new BlindnessRule("arbiter.ui", "arbiter.ui.adjudicator",
            TRUSTED_ENTRY_POINTS);

    private static final String FIXTURE = "arbiter.blindness.fixture";
    private static final BlindnessRule FIXTURE_RULE = new BlindnessRule(FIXTURE + ".ui", FIXTURE + ".ui.adjudicator",
            Set.of(FIXTURE + ".service.ScopedAnswers.forCurrentUser"));

    private static List<Violation> fixtureViolations;

    @BeforeAll
    static void checkFixtures() {
        fixtureViolations = FIXTURE_RULE.check(new ClassFileImporter().importPackages(FIXTURE));
    }

    @Test
    void annotatorBlindness_shippedCode_noViolations() {
        JavaClasses shipped = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("arbiter");
        assertFalse(SHIPPED.annotatorFacingClasses(shipped).isEmpty(), "no annotator-facing class was imported");

        List<Violation> violations = SHIPPED.check(shipped);

        assertEquals(List.of(), violations, "annotator-facing code reaches data blindness hides:\n"
                + violations.stream().map(Violation::toString).collect(Collectors.joining("\n")));
    }

    @Test
    void blindnessRule_answerReadInUnlistedUiPackage_reported() {
        assertReported(QueueScreen.class, Hidden.ANOTHER_ANNOTATORS_ANSWERS,
                "arbiter.data.annotation.AnnotationRepository.findByItemAndAnnotator(long, long)");
    }

    @Test
    void blindnessRule_resolutionReadInLambda_reported() {
        assertReported(ResolvedLabelScreen.class, Hidden.RESOLVED_RESULT,
                "arbiter.data.resolution.ResolutionRepository.findByItem(long)");
    }

    @Test
    void blindnessRule_progressReadTwoCallsDeep_reported() {
        Violation violation = assertReported(TeamProgressScreen.class, Hidden.CROSS_ANNOTATOR_PROGRESS,
                "arbiter.data.project.AssignmentRepository.listBySplit(long)");

        assertEquals(List.of(
                TeamProgressScreen.class.getName() + ".show(long)",
                FIXTURE + ".service.Progress.team(long)",
                FIXTURE + ".service.Progress.tally(long)",
                "arbiter.data.project.AssignmentRepository.listBySplit(long)"), violation.path());
    }

    @Test
    void blindnessRule_methodReference_reported() {
        assertReported(ReferenceScreen.class, Hidden.ANOTHER_ANNOTATORS_ANSWERS,
                "arbiter.data.annotation.AnnotationRepository.listByAssignment(long)");
    }

    @Test
    void blindnessRule_interfaceImplementation_reported() {
        // Only the non-adjudicator implementation is followed; ResolvedAnswers runs only after routing.
        assertReported(InterfaceScreen.class, Hidden.ANOTHER_ANNOTATORS_ANSWERS,
                "arbiter.data.annotation.AnnotationRepository.listByItem(long)");
    }

    @Test
    void blindnessRule_adjudicatorScreenReached_reported() {
        assertReported(AdjudicatorLinkScreen.class, Hidden.ADJUDICATOR_SCREEN,
                DisputeScreen.class.getName() + ".<init>(arbiter.data.json.JsonStore)");
    }

    @Test
    void blindnessRule_trustedEntryPointReturningResolution_reported() {
        assertReported(TrustedResultScreen.class, Hidden.RESOLVED_RESULT,
                "signature uses arbiter.model.resolution.Resolution");
    }

    @Test
    void blindnessRule_sharedViewModelTakingResolution_reported() {
        assertReported(ResolvedViewModel.class, Hidden.RESOLVED_RESULT,
                "signature uses arbiter.model.resolution.Resolution");
    }

    @Test
    void blindnessRule_resolutionReadFromUntypedValue_reported() {
        assertReported(UserDataScreen.class, Hidden.RESOLVED_RESULT,
                "arbiter.model.resolution.Resolution.getLabelId()");
    }

    @Test
    void blindnessRule_frameworkCallbackOfConstructedTask_reported() {
        Violation violation = assertReported(TaskScreen.class, Hidden.ANOTHER_ANNOTATORS_ANSWERS,
                "arbiter.data.annotation.AnnotationRepository.listByItem(long)");

        assertEquals(FIXTURE + ".service.LoadAnswersTask.call()", violation.path().get(1));
    }

    @Test
    void blindnessRule_trustedReadsAndSafeMethodsOfConstructedService_notReported() {
        assertEquals(List.of(), violationsFrom(BlindScreen.class));
    }

    @Test
    void blindnessRule_fixtures_onlyLeakingClassesReported() {
        Set<String> reported = fixtureViolations.stream().map(Violation::annotatorFacingClass)
                .collect(Collectors.toSet());

        assertEquals(Set.of(QueueScreen.class.getName(), ResolvedLabelScreen.class.getName(),
                TeamProgressScreen.class.getName(), ReferenceScreen.class.getName(),
                InterfaceScreen.class.getName(), AdjudicatorLinkScreen.class.getName(),
                TrustedResultScreen.class.getName(), ResolvedViewModel.class.getName(),
                UserDataScreen.class.getName(), TaskScreen.class.getName()), reported);
    }

    private static Violation assertReported(Class<?> leaking, Hidden hidden, String target) {
        List<Violation> violations = violationsFrom(leaking);
        assertEquals(1, violations.size(), "expected one violation from " + leaking.getSimpleName() + ": "
                + violations);
        Violation violation = violations.getFirst();
        assertEquals(hidden, violation.hidden());
        assertEquals(target, violation.target());
        return violation;
    }

    private static List<Violation> violationsFrom(Class<?> annotatorFacing) {
        return fixtureViolations.stream()
                .filter(violation -> violation.annotatorFacingClass().equals(annotatorFacing.getName()))
                .toList();
    }
}
