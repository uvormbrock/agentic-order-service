package com.example.agenticorderservice.architecture;

import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.customer.application.CustomerOrderAccess;
import com.example.agenticorderservice.customer.application.CustomerResult;
import com.example.agenticorderservice.customer.application.port.in.CustomerFacts;
import com.example.agenticorderservice.order.api.OrderController;
import com.example.agenticorderservice.order.application.*;
import com.example.agenticorderservice.order.application.port.in.OrderQueries;
import com.example.agenticorderservice.order.application.port.out.*;
import com.example.agenticorderservice.order.integration.OrderReadCustomerAdapter;
import com.example.agenticorderservice.order.persistence.OrderReadAdapter;
import com.example.agenticorderservice.order.persistence.OrderRepository;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OrderReadArchitectureTest {
    private static final String FEATURE = "com.example.agenticorderservice.order";
    private static final Class<?>[] MANIFEST = {
        OrderController.class, OrderQueries.class, OrderReadStore.class, OrderCustomerFacts.class,
        OrderQueryService.class, OrderReadSnapshot.class, OrderResult.class, OrderResult.Item.class,
        OrderReadAdapter.class, OrderReadCustomerAdapter.class
    };
    private static final JavaClasses SLICE = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importClasses(MANIFEST);

    @Test
    void importsApprovedProductionSliceAndItsImplementations() {
        for (Class<?> type : MANIFEST) {
            assertThat(SLICE.contain(type)).as(type.getName()).isTrue();
            assertThat(SLICE.get(type).reflect()).isEqualTo(type);
        }
        assertThat(OrderQueries.class.isAssignableFrom(OrderQueryService.class)).isTrue();
        assertThat(OrderReadStore.class.isAssignableFrom(OrderReadAdapter.class))
                .isTrue();
        assertThat(OrderCustomerFacts.class.isAssignableFrom(OrderReadCustomerAdapter.class))
                .isTrue();
        var serviceTargets = SLICE.get(OrderQueryService.class).getDirectDependenciesFromSelf().stream()
                .map(dependency -> dependency.getTargetClass().getName())
                .toList();
        assertThat(serviceTargets).contains(OrderReadStore.class.getName(), OrderCustomerFacts.class.getName());
        var bridgeTargets = SLICE.get(OrderReadCustomerAdapter.class).getDirectDependenciesFromSelf().stream()
                .map(dependency -> dependency.getTargetClass().getName())
                .toList();
        assertThat(bridgeTargets)
                .contains(CustomerFacts.class.getName())
                .doesNotContain(CustomerOrderAccess.class.getName());
    }

    @Test
    void applicationAndPortsHaveInwardDependencies() {
        HexagonalArchitectureRules.application(FEATURE).check(SLICE);
        HexagonalArchitectureRules.portInterfaces(FEATURE).check(SLICE);
        HexagonalArchitectureRules.portDependencies(FEATURE).check(SLICE);
        HexagonalArchitectureRules.api(FEATURE).check(SLICE);
        assertUseCaseBoundary(SLICE.get(OrderQueryService.class));
        assertAdapterBoundary(
                SLICE.get(OrderReadAdapter.class),
                Set.of(
                        OrderReadStore.class.getName(),
                        com.example.agenticorderservice.order.application.port.out.OrderReferenceStore.class.getName(),
                        OrderReadSnapshot.class.getName(),
                        OrderResult.class.getName(),
                        OrderResult.Item.class.getName()));
        assertAdapterBoundary(
                SLICE.get(OrderReadCustomerAdapter.class),
                Set.of(
                        OrderCustomerFacts.class.getName(),
                        CustomerFacts.class.getName(),
                        CustomerResult.class.getName()));
    }

    private static void assertUseCaseBoundary(JavaClass type) {
        for (var dependency : type.getDirectDependenciesFromSelf()) {
            String target = dependency.getTargetClass().getName();
            assertThat(target).doesNotContain(".persistence.", ".integration.");
            if (target.contains(".customer.application.")) assertThat(target).isEqualTo(CustomerResult.class.getName());
        }
    }

    private static void assertAdapterBoundary(JavaClass type, Set<String> allowedApplication) {
        for (var dependency : type.getDirectDependenciesFromSelf()) {
            var target = dependency.getTargetClass();
            assertThat(target.getPackageName())
                    .doesNotContain(
                            ".api",
                            ".customer.persistence",
                            ".customer.domain",
                            ".product.persistence",
                            ".product.domain");
            if (target.getPackageName().contains(".application")) {
                assertThat(allowedApplication)
                        .as(type.getName() + " -> " + target.getName())
                        .contains(target.getName());
            }
        }
    }

    @Test
    void controllerGetAndListCallInputPortWithoutConcreteBypasses() {
        assertReadBoundary(SLICE.get(OrderController.class));
    }

    private static void assertReadBoundary(JavaClass controller) {
        for (String operation : List.of("get", "list")) {
            var method = operation.equals("get")
                    ? controller.getMethod(operation, UUID.class)
                    : controller.getMethod(operation);
            assertThat(method.getMethodCallsFromSelf()).anySatisfy(call -> {
                assertThat(call.getTargetOwner().getName()).isEqualTo(OrderQueries.class.getName());
                assertThat(call.getTarget().getName()).isEqualTo(operation);
            });
            for (var access : method.getAccessesFromSelf()) {
                assertThat(access.getTargetOwner().getName())
                        .isNotEqualTo(OrderQueryService.class.getName())
                        .isNotEqualTo(OrderReadAdapter.class.getName())
                        .isNotEqualTo(OrderRepository.class.getName())
                        .isNotEqualTo(OrderPlacementCoordinator.class.getName());
            }
        }
        for (var dependency : controller.getDirectDependenciesFromSelf()) {
            assertThat(dependency.getTargetClass().getName())
                    .isNotEqualTo(OrderQueryService.class.getName())
                    .isNotEqualTo(OrderReadAdapter.class.getName())
                    .isNotEqualTo(OrderRepository.class.getName());
        }
    }

    @Test
    void recursivePublicContractsAreTransportNeutralAndEntityFree() {
        for (Class<?> port : List.of(OrderQueries.class, OrderReadStore.class, OrderCustomerFacts.class))
            assertContract(port);
        assertType(OrderReadSnapshot.class, new HashSet<>());
    }

    private static void assertContract(Class<?> port) {
        assertThat(port.isInterface()).isTrue();
        for (Method method : port.getMethods()) {
            assertType(method.getGenericReturnType(), new HashSet<>());
            for (Type type : method.getGenericParameterTypes()) assertType(type, new HashSet<>());
        }
    }

    private static void assertType(Type type, Set<Type> visited) {
        if (!visited.add(type)) return;
        if (type instanceof ParameterizedType generic) {
            assertType(generic.getRawType(), visited);
            for (Type argument : generic.getActualTypeArguments()) assertType(argument, visited);
        } else if (type instanceof Class<?> concrete) {
            assertThat(concrete.isAnnotationPresent(jakarta.persistence.Entity.class))
                    .as(concrete.getName())
                    .isFalse();
            for (String forbidden : List.of(
                    "org.springframework.",
                    "jakarta.persistence.",
                    "jakarta.servlet.",
                    "org.hibernate.",
                    "java.sql.",
                    "javax.sql.",
                    "java.io.",
                    "java.nio.file.")) {
                assertThat(concrete.getName()).doesNotStartWith(forbidden);
            }
            if (concrete.isArray()) assertType(concrete.getComponentType(), visited);
            if (concrete.isRecord())
                for (RecordComponent component : concrete.getRecordComponents())
                    assertType(component.getGenericType(), visited);
        } else if (type instanceof WildcardType wildcard) {
            for (Type bound : wildcard.getUpperBounds()) assertType(bound, visited);
            for (Type bound : wildcard.getLowerBounds()) assertType(bound, visited);
        } else if (type instanceof TypeVariable<?> variable) {
            for (Type bound : variable.getBounds()) assertType(bound, visited);
        } else if (type instanceof GenericArrayType array) assertType(array.getGenericComponentType(), visited);
    }

    @Test
    void rejectsConcreteControllerAndForeignUseCaseBypassesEvenWithPortCall() {
        var invalid = new ClassFileImporter()
                .importClasses(
                        ConcreteBypass.class,
                        ForeignProviderBypass.class,
                        AdapterBypass.class,
                        BridgePersistenceBypass.class);
        assertThatThrownBy(() -> assertReadBoundary(invalid.get(ConcreteBypass.class)))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertUseCaseBoundary(invalid.get(ForeignProviderBypass.class)))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() ->
                        assertAdapterBoundary(invalid.get(AdapterBypass.class), Set.of(OrderReadStore.class.getName())))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void rejectsBridgeForeignPersistence() {
        var imported = new ClassFileImporter().importClasses(BridgePersistenceBypass.class);
        assertThatThrownBy(() -> assertAdapterBoundary(imported.get(BridgePersistenceBypass.class), Set.of()))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void rejectsNestedEntityHttpAndInfrastructureContractLeaks() {
        for (Class<?> invalid : List.of(EntityLeak.class, HttpLeak.class, InfrastructureLeak.class)) {
            assertThatThrownBy(() -> assertContract(invalid)).isInstanceOf(AssertionError.class);
        }
    }

    record NestedEntity(com.example.agenticorderservice.order.domain.Order entity) {}

    interface EntityLeak {
        List<NestedEntity> get();
    }

    interface HttpLeak {
        org.springframework.http.ResponseEntity<OrderResult> get();
    }

    interface InfrastructureLeak {
        Optional<java.sql.Connection> get();
    }

    static class ConcreteBypass {
        OrderQueries input;
        OrderQueryService concrete;

        public OrderResult get(UUID id) {
            input.get(id);
            return concrete.get(id);
        }

        public List<OrderResult> list() {
            return input.list();
        }
    }

    static class ForeignProviderBypass {
        CustomerFacts provider;

        public Map<UUID, CustomerResult> current(Collection<UUID> ids) {
            return provider.current(ids);
        }
    }

    static class BridgePersistenceBypass {
        com.example.agenticorderservice.customer.persistence.CustomerRepository repository;
    }

    static class AdapterBypass {
        OrderQueryService useCase;

        public OrderResult get(UUID id) {
            return useCase.get(id);
        }
    }
}
