package com.example.agenticorderservice.architecture;

import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.customer.application.port.out.CustomerWriteStore;
import com.example.agenticorderservice.order.application.port.out.OrderWriteStore;
import com.example.agenticorderservice.product.application.port.out.ProductWriteStore;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

/** Whole-production enforcement of ADR-0007, including its three exact aggregate-port exceptions. */
class HexagonalConsolidationArchitectureTest {
    private static final String ROOT = "com.example.agenticorderservice";
    private static final List<String> FEATURES = List.of("product", "customer", "order");
    private static final JavaClasses PRODUCTION = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void everyMigratedFeatureHasRealProductionScopesAndInwardApplicationDependencies() {
        for (String feature : FEATURES) {
            String prefix = ROOT + "." + feature;
            for (String scope : List.of(
                    ".application", ".application.port.in", ".application.port.out", ".persistence", ".api", ".domain"))
                assertThat(typesIn(prefix + scope)).as(prefix + scope).isNotEmpty();
            HexagonalArchitectureRules.domain(prefix).check(PRODUCTION);
            HexagonalArchitectureRules.application(prefix).check(PRODUCTION);
            HexagonalArchitectureRules.api(prefix).check(PRODUCTION);
            HexagonalArchitectureRules.portInterfaces(prefix).check(PRODUCTION);
        }
    }

    private static List<Class<?>> typesIn(String prefix) {
        return PRODUCTION.stream()
                .filter(type -> type.getPackageName().equals(prefix)
                        || type.getPackageName().startsWith(prefix + "."))
                .map(JavaClass::reflect)
                .toList();
    }

    @Test
    void inputPortsHaveRealImplementationsAndOrdinaryPortsRetainDependencyChecks() {
        var inputs = PRODUCTION.stream()
                .filter(type -> type.getPackageName().endsWith(".application.port.in"))
                .map(JavaClass::reflect)
                .toList();
        var services = PRODUCTION.stream()
                .filter(type -> type.getPackageName().endsWith(".application") && component(type.reflect()))
                .toList();
        assertThat(inputs).isNotEmpty();
        for (Class<?> port : inputs)
            assertThat(services)
                    .as(port.getName())
                    .anySatisfy(service ->
                            assertThat(port.isAssignableFrom(service.reflect())).isTrue());
        for (String feature : FEATURES) {
            var ordinary = PRODUCTION.stream()
                    .filter(type -> type.getPackageName().startsWith(ROOT + "." + feature + ".application.port."))
                    .map(JavaClass::reflect)
                    .filter(type -> type != ProductWriteStore.class
                            && type != CustomerWriteStore.class
                            && type != OrderWriteStore.class)
                    .toList();
            assertThat(ordinary).isNotEmpty();
            HexagonalArchitectureRules.portDependencies(ROOT + "." + feature)
                    .check(new ClassFileImporter().importClasses(ordinary));
        }
    }

    @Test
    void applicationTransactionRuntimeAccessRetainsOnlyCustomerCompletionObservation() {
        for (var origin : PRODUCTION) if (origin.getPackageName().contains(".application")) transactionBoundary(origin);
    }

    static void transactionBoundary(JavaClass origin) {
        for (var dependency : origin.getDirectDependenciesFromSelf()) {
            var target = dependency.getTargetClass();
            if (!target.getPackageName().startsWith("org.springframework.transaction")) continue;
            if (target.getPackageName().equals("org.springframework.transaction.annotation")) continue;
            assertThat(origin.getName().equals(ROOT + ".customer.application.CustomerApplicationService")
                            || origin.getName().equals(ROOT + ".customer.application.CustomerApplicationService$1"))
                    .as(dependency.getDescription())
                    .isTrue();
            assertThat(target.getName())
                    .isIn(
                            "org.springframework.transaction.support.TransactionSynchronizationManager",
                            "org.springframework.transaction.support.TransactionSynchronization");
        }
    }

    @Test
    void allControllerConstructorsUseInputPortsAndEveryEndpointDelegatesToOne() {
        var controllers = PRODUCTION.stream()
                .filter(type -> type.isAnnotatedWith(RestController.class) && feature(type.getPackageName()) != null)
                .toList();
        assertThat(controllers).hasSize(4);
        for (var controller : controllers) {
            if (!FEATURES.stream()
                    .anyMatch(feature -> controller.getPackageName().startsWith(ROOT + "." + feature + "."))) continue;
            controllerBoundary(controller);
        }
    }

    static void controllerBoundary(JavaClass controller) {
        for (var dependency : controller.getDirectDependenciesFromSelf()) {
            var target = dependency.getTargetClass();
            if (target.getPackageName().contains(".application.")
                    || target.getPackageName().endsWith(".application"))
                assertThat(component(target.reflect()))
                        .as(dependency.getDescription())
                        .isFalse();
        }
        for (var constructor : controller.reflect().getDeclaredConstructors())
            for (Class<?> parameter : constructor.getParameterTypes()) {
                assertThat(parameter.isInterface()).as(parameter.getName()).isTrue();
                assertThat(parameter.getPackageName()).endsWith(".application.port.in");
            }
        for (var method : controller.getMethods()) {
            if (!method.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.PUBLIC)) continue;
            if (method.getAnnotations().stream()
                    .noneMatch(annotation ->
                            annotation.getRawType().getName().startsWith("org.springframework.web.bind.annotation.")))
                continue;
            assertThat(method.getMethodCallsFromSelf())
                    .as(method.getFullName())
                    .anySatisfy(call ->
                            assertThat(call.getTargetOwner().getPackageName()).endsWith(".application.port.in"));
        }
    }

    @Test
    void sharedHttpValidationRetainsOnlyTheTwoExactSupportedStaticValidationCalls() {
        var validationCalls = new ArrayList<String>();
        for (var origin : PRODUCTION) {
            if (!origin.getPackageName().startsWith(ROOT + ".shared.api")) continue;
            for (var call : origin.getMethodCallsFromSelf()) {
                Class<?> target = call.getTargetOwner().reflect();
                if (!target.getPackageName().contains(".application") || !component(target)) continue;
                assertThat(origin.getName()).isEqualTo(ROOT + ".shared.api.StrictJsonRequestDecoder");
                String signature =
                        call.getTargetOwner().getName() + "." + call.getTarget().getName() + "("
                                + String.join(
                                        ",",
                                        call.getTarget().getRawParameterTypes().stream()
                                                .map(JavaClass::getName)
                                                .toList())
                                + ")";
                assertThat(signature)
                        .isIn(
                                ROOT
                                        + ".product.application.ProductApplicationService.validatePrice(java.math.BigDecimal)",
                                ROOT
                                        + ".customer.application.CustomerApplicationService.normalizeEmail(java.lang.String)");
                validationCalls.add(signature);
            }
            for (var field : origin.getFields()) {
                Class<?> target = field.getRawType().reflect();
                if (target.getPackageName().contains(".application"))
                    assertThat(component(target)).isFalse();
            }
        }
        assertThat(validationCalls)
                .containsExactlyInAnyOrder(
                        ROOT + ".product.application.ProductApplicationService.validatePrice(java.math.BigDecimal)",
                        ROOT + ".customer.application.CustomerApplicationService.normalizeEmail(java.lang.String)");
    }

    @Test
    void everyOutputPortHasAnAdapterAndOutboundComponentsCannotCallUseCases() {
        var ports = PRODUCTION.stream()
                .filter(type -> type.getPackageName().endsWith(".application.port.out"))
                .map(JavaClass::reflect)
                .toList();
        var adapters = PRODUCTION.stream()
                .filter(type -> outbound(type.getPackageName()) && component(type.reflect()))
                .toList();
        assertThat(ports).isNotEmpty();
        assertThat(adapters).isNotEmpty();
        for (Class<?> port : ports)
            assertThat(adapters)
                    .as(port.getName())
                    .anySatisfy(adapter ->
                            assertThat(port.isAssignableFrom(adapter.reflect())).isTrue());
        for (var adapter : adapters) {
            // Infrastructure-only guard is a collaborator of the transaction adapter, not a capability adapter.
            if (!adapter.getName().equals(ROOT + ".order.transaction.AmbientTransactionGuard"))
                assertThat(ports)
                        .as(adapter.getName())
                        .anySatisfy(port -> assertThat(port.isAssignableFrom(adapter.reflect()))
                                .isTrue());
            adapterBoundary(adapter);
        }
    }

    private static boolean outbound(String name) {
        return List.of(".persistence", ".storage", ".integration", ".transaction").stream()
                .anyMatch(part -> name.endsWith(part) || name.contains(part + "."));
    }

    static void adapterBoundary(JavaClass adapter) {
        for (var dependency : adapter.getDirectDependenciesFromSelf()) {
            var target = dependency.getTargetClass();
            if (target.getPackageName().contains(".application"))
                assertThat(component(target.reflect()))
                        .as(dependency.getDescription())
                        .isFalse();
        }
    }

    @Test
    void foreignFeatureAccessUsesOnlyProviderCapabilitiesAndImmutableFactsOrTypedFailures() {
        for (var origin : PRODUCTION) crossFeatureBoundary(origin);
    }

    static void crossFeatureBoundary(JavaClass origin) {
        String owner = feature(origin.getPackageName());
        if (owner == null) return;
        for (var dependency : origin.getDirectDependenciesFromSelf()) {
            var target = dependency.getTargetClass();
            String other = feature(target.getPackageName());
            if (other == null || owner.equals(other)) continue;
            String providerApplication = ROOT + "." + other + ".application";
            assertThat(target.getPackageName()).as(dependency.getDescription()).startsWith(providerApplication);
            Class<?> type = target.reflect();
            if (target.getPackageName().equals(providerApplication + ".port.in")) {
                assertThat(outbound(origin.getPackageName()))
                        .as(dependency.getDescription())
                        .isTrue();
                assertThat(type.isInterface()).isTrue();
            } else {
                assertThat(type.isRecord() || type.isEnum() || RuntimeException.class.isAssignableFrom(type))
                        .as(dependency.getDescription())
                        .isTrue();
            }
        }
    }

    private static String feature(String name) {
        return FEATURES.stream()
                .filter(feature -> name.startsWith(ROOT + "." + feature + "."))
                .findFirst()
                .orElse(null);
    }

    @Test
    void allPortSignaturesRemainNeutralWithOnlyExactApprovedAggregateMethods() {
        var ports = PRODUCTION.stream()
                .filter(type -> type.getPackageName().matches(".*\\.application\\.port\\.(in|out)"))
                .map(JavaClass::reflect)
                .toList();
        assertThat(ports).isNotEmpty();
        for (Class<?> port : ports) contract(port);
    }

    static void contract(Class<?> port) {
        assertThat(port.isInterface()).isTrue();
        for (Method method : port.getMethods()) {
            Class<?> aggregate = port == ProductWriteStore.class
                    ? com.example.agenticorderservice.product.domain.Product.class
                    : port == CustomerWriteStore.class
                            ? com.example.agenticorderservice.customer.domain.Customer.class
                            : port == OrderWriteStore.class
                                    ? com.example.agenticorderservice.order.domain.Order.class
                                    : null;
            boolean approved = aggregate != null
                    && (method.getName().equals("save")
                                    && method.getReturnType() == aggregate
                                    && Arrays.equals(method.getParameterTypes(), new Class<?>[] {aggregate})
                            || port != OrderWriteStore.class
                                    && method.getName().equals("findLockedById")
                                    && Arrays.equals(method.getParameterTypes(), new Class<?>[] {UUID.class})
                                    && method.getGenericReturnType()
                                            .getTypeName()
                                            .equals("java.util.Optional<" + aggregate.getName() + ">")
                            || port == CustomerWriteStore.class
                                    && method.getName().equals("delete")
                                    && method.getReturnType() == void.class
                                    && Arrays.equals(method.getParameterTypes(), new Class<?>[] {aggregate}));
            if (approved) continue;
            neutral(method.getGenericReturnType(), new HashSet<>());
            for (Type type : method.getGenericParameterTypes()) neutral(type, new HashSet<>());
            for (Type type : method.getGenericExceptionTypes()) neutral(type, new HashSet<>());
        }
    }

    private static void neutral(Type type, Set<Type> seen) {
        if (!seen.add(type)) return;
        if (type instanceof ParameterizedType parameterized) {
            neutral(parameterized.getRawType(), seen);
            for (Type argument : parameterized.getActualTypeArguments()) neutral(argument, seen);
        } else if (type instanceof Class<?> concrete) {
            assertThat(concrete.isAnnotationPresent(jakarta.persistence.Entity.class))
                    .as(concrete.getName())
                    .isFalse();
            for (String prefix : List.of(
                    "org.springframework.",
                    "jakarta.persistence.",
                    "jakarta.servlet.",
                    "org.hibernate.",
                    "java.sql.",
                    "javax.sql.",
                    "java.io.",
                    "java.nio.file.",
                    "javax.imageio.")) assertThat(concrete.getName()).doesNotStartWith(prefix);
            assertThat(outbound(concrete.getPackageName()))
                    .as(concrete.getName())
                    .isFalse();
            if (concrete.isArray()) neutral(concrete.getComponentType(), seen);
            if (concrete.isRecord())
                for (RecordComponent field : concrete.getRecordComponents()) neutral(field.getGenericType(), seen);
        } else if (type instanceof WildcardType wildcard) {
            for (Type bound : wildcard.getUpperBounds()) neutral(bound, seen);
            for (Type bound : wildcard.getLowerBounds()) neutral(bound, seen);
        } else if (type instanceof TypeVariable<?> variable) {
            for (Type bound : variable.getBounds()) neutral(bound, seen);
        } else if (type instanceof GenericArrayType array) neutral(array.getGenericComponentType(), seen);
    }

    @Test
    void productionComponentConstructorsAreAcyclicIncludingInterfaceImplementations() {
        var components = PRODUCTION.stream()
                .map(JavaClass::reflect)
                .filter(HexagonalConsolidationArchitectureTest::component)
                .toList();
        assertThat(components.size()).isGreaterThan(20);
        noConstructorCycles(components);
    }

    private static boolean component(Class<?> type) {
        return !type.isInterface() && AnnotatedElementUtils.hasAnnotation(type, Component.class);
    }

    static void noConstructorCycles(List<Class<?>> components) {
        var graph = new LinkedHashMap<Class<?>, Set<Class<?>>>();
        for (Class<?> type : components) {
            var dependencies = new HashSet<Class<?>>();
            for (Constructor<?> constructor : type.getDeclaredConstructors())
                for (Class<?> parameter : constructor.getParameterTypes())
                    for (Class<?> candidate : components)
                        if (parameter.isAssignableFrom(candidate)) dependencies.add(candidate);
            graph.put(type, dependencies);
        }
        for (Class<?> type : components) visit(type, graph, new LinkedHashSet<>(), new HashSet<>());
    }

    private static void visit(
            Class<?> type, Map<Class<?>, Set<Class<?>>> graph, Set<Class<?>> active, Set<Class<?>> done) {
        assertThat(active)
                .as("constructor dependency cycle reaching %s", type.getName())
                .doesNotContain(type);
        if (done.contains(type)) return;
        active.add(type);
        for (Class<?> dependency : graph.getOrDefault(type, Set.of())) visit(dependency, graph, active, done);
        active.remove(type);
        done.add(type);
    }

    @Test
    void rejectsConcreteControllerProviderAndAdapterBypassesAndNestedPortLeaks() {
        var fixtures = new ClassFileImporter()
                .importClasses(
                        ConcreteController.class,
                        ConcreteAdapter.class,
                        com.example.agenticorderservice.order.integration.ForeignProviderBypassFixture.class);
        assertThatThrownBy(() -> controllerBoundary(fixtures.get(ConcreteController.class)))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> adapterBoundary(fixtures.get(ConcreteAdapter.class)))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> crossFeatureBoundary(fixtures.get(
                        com.example.agenticorderservice.order.integration.ForeignProviderBypassFixture.class)))
                .isInstanceOf(AssertionError.class);
        var transaction = new ClassFileImporter().importClasses(TransactionRuntimeBypass.class);
        assertThatThrownBy(() -> transactionBoundary(transaction.get(TransactionRuntimeBypass.class)))
                .isInstanceOf(AssertionError.class);
        for (Class<?> port :
                List.of(NestedEntityLeak.class, HttpLeak.class, AggregateSaveLookalike.class, GenericArrayLeak.class))
            assertThatThrownBy(() -> contract(port)).isInstanceOf(AssertionError.class);
    }

    @Test
    void detectsConstructorCyclesThroughCapabilitiesAndAcceptsAcyclicComponents() {
        noConstructorCycles(List.of(Acyclic.class));
        assertThatThrownBy(() -> noConstructorCycles(List.of(CycleA.class, CycleB.class)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(CycleA.class.getName());
    }

    static class TransactionRuntimeBypass {
        org.springframework.transaction.support.TransactionTemplate template;
    }

    static class ConcreteController {
        com.example.agenticorderservice.product.application.port.in.ListProductsQuery input;
        com.example.agenticorderservice.product.application.ProductApplicationService concrete;
    }

    static class ConcreteAdapter {
        com.example.agenticorderservice.order.application.OrderQueryService concrete;
    }

    record NestedEntity(com.example.agenticorderservice.product.domain.Product product) {}

    interface NestedEntityLeak {
        List<NestedEntity> get();
    }

    interface HttpLeak {
        org.springframework.http.ResponseEntity<String> get();
    }

    interface AggregateSaveLookalike {
        com.example.agenticorderservice.order.domain.Order save(
                com.example.agenticorderservice.order.domain.Order order);
    }

    interface GenericArrayLeak<T extends org.springframework.http.ResponseEntity<String>> {
        T[] get();
    }

    interface CapabilityA {}

    interface CapabilityB {}

    static class CycleA implements CapabilityA {
        CycleA(CapabilityB dependency) {}
    }

    static class CycleB implements CapabilityB {
        CycleB(CapabilityA dependency) {}
    }

    static class Acyclic {
        Acyclic(java.time.Clock clock) {}
    }
}
