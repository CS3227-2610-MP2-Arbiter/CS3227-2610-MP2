package arbiter.blindness;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaType;

/**
 * Finds every path from annotator-facing code to data that blindness hides (rule 1).
 *
 * <p>Annotator-facing code is every class in the UI package except the adjudicator's screens, so a
 * shared component or a new UI package is covered without being named. The walk starts from each
 * of those classes' methods and follows calls, constructor calls, method references and lambda
 * bodies into other application code, including every implementation of an interface or
 * overridable method it calls, except implementations in the adjudicator's package, which run only
 * after routing to that role. Constructing an application class also reaches its methods that
 * override a JDK or JavaFX method, such as {@code Task.call}, because the framework calls those
 * rather than the application. It stops at the repository interfaces, the model and the workspace
 * layer, and at the trusted scoped entry points, whose scoping their own features test; their
 * signatures are still checked.
 *
 * <p>Any use of a resolution type is a violation, whether in a signature, a call, a constructor or
 * a field access, so a resolution handed over untyped (as JavaFX's {@code getUserData} returns one)
 * is still caught when it is read.
 *
 * <p>A violation is reported once, along the first path found to it.
 */
final class BlindnessRule {
    private static final String ANSWERS = "arbiter.data.annotation.AnnotationRepository";
    private static final String RESOLUTIONS = "arbiter.data.resolution.ResolutionRepository";
    private static final String ASSIGNMENTS = "arbiter.data.project.AssignmentRepository";
    private static final Set<String> PROGRESS_READS = Set.of("listBySplit", "listByStatus");
    private static final String RESOLUTION_MODEL = "arbiter.model.resolution";
    private static final List<String> NOT_ENTERED = List.of("arbiter.data", "arbiter.model", "arbiter.workspace");

    private final String uiPackage;
    private final String adjudicatorPackage;
    private final Set<String> trustedEntryPoints;

    /**
     * Creates a rule for one code base.
     *
     * @param uiPackage the package whose classes, at any depth, face the annotator
     * @param adjudicatorPackage the package of the adjudicator's screens inside {@code uiPackage}
     * @param trustedEntryPoints fully qualified {@code Class.method} names of the scoped reads the walk
     *     does not enter, covering every overload
     */
    BlindnessRule(String uiPackage, String adjudicatorPackage, Set<String> trustedEntryPoints) {
        this.uiPackage = uiPackage;
        this.adjudicatorPackage = adjudicatorPackage;
        this.trustedEntryPoints = Set.copyOf(trustedEntryPoints);
    }

    /** Returns the names of the classes the walk starts from. */
    Set<String> annotatorFacingClasses(JavaClasses classes) {
        Set<String> names = new LinkedHashSet<>();
        for (JavaClass javaClass : classes) {
            if (isAnnotatorFacing(javaClass)) {
                names.add(javaClass.getName());
            }
        }
        return names;
    }

    /** Returns every violation found in these classes, in the order the walk met them. */
    List<Violation> check(JavaClasses classes) {
        Map<JavaCodeUnit, JavaCodeUnit> caller = new HashMap<>();
        Deque<JavaCodeUnit> pending = new ArrayDeque<>();
        for (JavaClass javaClass : classes) {
            if (isAnnotatorFacing(javaClass)) {
                for (JavaCodeUnit unit : javaClass.getCodeUnits()) {
                    caller.put(unit, null);
                    pending.add(unit);
                }
            }
        }
        List<Violation> violations = new ArrayList<>();
        while (!pending.isEmpty()) {
            JavaCodeUnit unit = pending.poll();
            checkSignature(unit, List.of(), caller, violations);
            List<JavaCodeUnitAccess<?>> accesses = new ArrayList<>(unit.getCallsFromSelf());
            accesses.addAll(unit.getCodeUnitReferencesFromSelf());
            for (JavaCodeUnitAccess<?> access : accesses) {
                CodeUnitAccessTarget target = access.getTarget();
                Hidden hidden = hiddenBy(target);
                if (hidden != null) {
                    violations.add(violation(unit, List.of(target.getFullName()), hidden, caller));
                } else if (trustedEntryPoints.contains(target.getOwner().getName() + "." + target.getName())) {
                    target.resolveMember().ifPresent(trusted ->
                            checkSignature(unit, List.of(trusted.getFullName()), trusted, caller, violations));
                } else if (isEntered(target.getOwner())) {
                    List<JavaCodeUnit> reached = implementations(target);
                    if (target.getName().equals(JavaConstructor.CONSTRUCTOR_NAME)) {
                        reached.addAll(frameworkCallbacks(target.getOwner()));
                    }
                    for (JavaCodeUnit next : reached) {
                        if (!caller.containsKey(next)) {
                            caller.put(next, unit);
                            pending.add(next);
                        }
                    }
                }
            }
            for (JavaFieldAccess access : unit.getFieldAccesses()) {
                FieldAccessTarget field = access.getTarget();
                if (inPackage(field.getOwner(), RESOLUTION_MODEL) || inPackage(field.getRawType(), RESOLUTION_MODEL)) {
                    violations.add(violation(unit, List.of(field.getFullName()), Hidden.RESOLVED_RESULT, caller));
                }
            }
        }
        return violations;
    }

    private Hidden hiddenBy(CodeUnitAccessTarget target) {
        JavaClass owner = target.getOwner();
        if (inPackage(owner, adjudicatorPackage)) {
            return Hidden.ADJUDICATOR_SCREEN;
        }
        if (inPackage(owner, RESOLUTION_MODEL)) {
            return Hidden.RESOLVED_RESULT;
        }
        if (owner.isAssignableTo(ANSWERS) && !target.getName().equals("insert")) {
            return Hidden.ANOTHER_ANNOTATORS_ANSWERS;
        }
        if (owner.isAssignableTo(RESOLUTIONS)) {
            return Hidden.RESOLVED_RESULT;
        }
        if (owner.isAssignableTo(ASSIGNMENTS) && PROGRESS_READS.contains(target.getName())) {
            return Hidden.CROSS_ANNOTATOR_PROGRESS;
        }
        return null;
    }

    private void checkSignature(JavaCodeUnit unit, List<String> via, Map<JavaCodeUnit, JavaCodeUnit> caller,
            List<Violation> violations) {
        checkSignature(unit, via, unit, caller, violations);
    }

    private void checkSignature(JavaCodeUnit reached, List<String> via, JavaCodeUnit checked,
            Map<JavaCodeUnit, JavaCodeUnit> caller, List<Violation> violations) {
        List<JavaType> types = new ArrayList<>(checked.getParameterTypes());
        types.add(checked.getReturnType());
        for (JavaType type : types) {
            for (JavaClass involved : type.getAllInvolvedRawTypes()) {
                if (inPackage(involved, RESOLUTION_MODEL)) {
                    List<String> path = new ArrayList<>(via);
                    path.add("signature uses " + involved.getName());
                    violations.add(violation(reached, path, Hidden.RESOLVED_RESULT, caller));
                    return;
                }
            }
        }
    }

    private List<JavaCodeUnit> implementations(CodeUnitAccessTarget target) {
        List<JavaCodeUnit> found = new ArrayList<>();
        target.resolveMember().ifPresent(found::add);
        boolean overridable = found.stream().noneMatch(unit -> unit.getModifiers().contains(JavaModifier.STATIC))
                && !target.getName().equals(JavaConstructor.CONSTRUCTOR_NAME);
        if (overridable) {
            String[] parameters = target.getRawParameterTypes().stream().map(JavaClass::getName).toArray(String[]::new);
            for (JavaClass subclass : target.getOwner().getAllSubclasses()) {
                if (isEntered(subclass)) {
                    subclass.tryGetMethod(target.getName(), parameters).ifPresent(found::add);
                }
            }
        }
        return found;
    }

    private List<JavaCodeUnit> frameworkCallbacks(JavaClass constructed) {
        List<JavaClass> hierarchy = new ArrayList<>(List.of(constructed));
        constructed.getAllRawSuperclasses().stream().filter(this::isEntered).forEach(hierarchy::add);
        List<JavaClass> framework = new ArrayList<>(constructed.getAllRawSuperclasses());
        framework.addAll(constructed.getAllRawInterfaces());
        framework.removeIf(type -> inPackage(type, "arbiter"));
        List<JavaCodeUnit> callbacks = new ArrayList<>();
        for (JavaClass type : hierarchy) {
            for (JavaMethod method : type.getMethods()) {
                String[] parameters = method.getRawParameterTypes().stream().map(JavaClass::getName)
                        .toArray(String[]::new);
                boolean overridesFramework = framework.stream()
                        .anyMatch(supertype -> supertype.tryGetMethod(method.getName(), parameters).isPresent());
                if (overridesFramework && !method.getModifiers().contains(JavaModifier.STATIC)) {
                    callbacks.add(method);
                }
            }
        }
        return callbacks;
    }

    private Violation violation(JavaCodeUnit reached, List<String> tail, Hidden hidden,
            Map<JavaCodeUnit, JavaCodeUnit> caller) {
        List<String> path = new ArrayList<>();
        JavaCodeUnit root = reached;
        for (JavaCodeUnit step = reached; step != null; step = caller.get(step)) {
            path.addFirst(step.getFullName());
            root = step;
        }
        path.addAll(tail);
        return new Violation(root.getOwner().getName(), hidden, List.copyOf(path));
    }

    private boolean isAnnotatorFacing(JavaClass javaClass) {
        return inPackage(javaClass, uiPackage) && !inPackage(javaClass, adjudicatorPackage);
    }

    private boolean isEntered(JavaClass javaClass) {
        return inPackage(javaClass, "arbiter") && !inPackage(javaClass, adjudicatorPackage)
                && NOT_ENTERED.stream().noneMatch(excluded -> inPackage(javaClass, excluded));
    }

    private static boolean inPackage(JavaClass javaClass, String packageName) {
        String name = javaClass.getPackageName();
        return name.equals(packageName) || name.startsWith(packageName + ".");
    }

    /** What an annotator must never see, as rule 1 lists it. */
    enum Hidden {
        ANOTHER_ANNOTATORS_ANSWERS,
        RESOLVED_RESULT,
        CROSS_ANNOTATOR_PROGRESS,
        ADJUDICATOR_SCREEN
    }

    /**
     * One path from annotator-facing code to hidden data.
     *
     * @param annotatorFacingClass the class the path starts in
     * @param hidden what the path reaches
     * @param path the methods along the path, ending with the access that reaches hidden data
     */
    record Violation(String annotatorFacingClass, Hidden hidden, List<String> path) {
        /** Returns the last step of the path, the access that reaches hidden data. */
        String target() {
            return path.getLast();
        }

        @Override
        public String toString() {
            return hidden + ": " + String.join(" -> ", path);
        }
    }
}
