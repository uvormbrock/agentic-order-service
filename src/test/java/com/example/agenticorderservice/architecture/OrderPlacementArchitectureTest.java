package com.example.agenticorderservice.architecture;

import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.customer.application.CustomerResult;
import com.example.agenticorderservice.customer.application.port.in.CustomerFacts;
import com.example.agenticorderservice.order.api.OrderController;
import com.example.agenticorderservice.order.application.*;
import com.example.agenticorderservice.order.application.port.in.*;
import com.example.agenticorderservice.order.application.port.out.*;
import com.example.agenticorderservice.order.domain.Order;
import com.example.agenticorderservice.order.integration.*;
import com.example.agenticorderservice.order.persistence.*;
import com.example.agenticorderservice.order.transaction.*;
import com.example.agenticorderservice.product.application.ProductResult;
import com.example.agenticorderservice.product.application.port.in.ProductFacts;
import com.tngtech.archunit.core.domain.*;
import com.tngtech.archunit.core.importer.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OrderPlacementArchitectureTest {
    static final Class<?>[] MANIFEST = {
        OrderController.class,
        OrderPlacementCoordinator.class,
        PlacementAttempt.class,
        PlacementAttempt.Completion.class,
        OrderPlacementOperations.class,
        OrderWriteStore.class,
        OrderPlacementAttempts.class,
        OrderPlacementCustomerFacts.class,
        OrderPlacementProductFacts.class,
        OrderWriteAdapter.class,
        OrderPlacementCustomerAdapter.class,
        OrderPlacementProductAdapter.class,
        SpringOrderPlacementAttempts.class
    };
    static final JavaClasses SLICE = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importClasses(MANIFEST);

    @Test
    void importsPopulatedProductionSliceAndAllImplementations() {
        for (Class<?> type : MANIFEST) assertThat(SLICE.get(type).reflect()).isEqualTo(type);
        assertThat(OrderPlacementOperations.class.isAssignableFrom(OrderPlacementCoordinator.class))
                .isTrue();
        assertThat(OrderWriteStore.class.isAssignableFrom(OrderWriteAdapter.class))
                .isTrue();
        assertThat(OrderPlacementAttempts.class.isAssignableFrom(SpringOrderPlacementAttempts.class))
                .isTrue();
        assertThat(OrderPlacementCustomerFacts.class.isAssignableFrom(OrderPlacementCustomerAdapter.class))
                .isTrue();
        assertThat(OrderPlacementProductFacts.class.isAssignableFrom(OrderPlacementProductAdapter.class))
                .isTrue();
        assertThat(ProductFacts.class.isAssignableFrom(
                        com.example.agenticorderservice.product.application.ProductOrderAccess.class))
                .isTrue();
    }

    @Test
    void coordinatorAndControllerUseApprovedContracts() {
        boundary(
                SLICE.get(OrderPlacementCoordinator.class),
                Set.of(
                        OrderPlacementOperations.class.getName(),
                        OrderPlacementCustomerFacts.class.getName(),
                        OrderPlacementProductFacts.class.getName(),
                        OrderWriteStore.class.getName(),
                        OrderPlacementAttempts.class.getName(),
                        OrderResult.class.getName(),
                        PlaceOrderCommand.class.getName(),
                        PlaceOrderCommand.Line.class.getName(),
                        PlacementAttempt.class.getName(),
                        PlacementAttempt.Completion.class.getName(),
                        OrderFailure.class.getName(),
                        OrderFailure.Reason.class.getName(),
                        CustomerResult.class.getName(),
                        ProductResult.class.getName(),
                        com.example.agenticorderservice.customer.application.CustomerNotFoundException.class.getName(),
                        com.example.agenticorderservice.product.application.ProductNotFoundException.class.getName()),
                true);
        controller(SLICE.get(OrderController.class));
    }

    static void controller(JavaClass type) {
        assertThat(type.getMethodCallsFromSelf()).anySatisfy(call -> {
            assertThat(call.getTargetOwner().getName()).isEqualTo(OrderPlacementOperations.class.getName());
            assertThat(call.getTarget().getName()).isEqualTo("place");
        });
        for (var dep : type.getDirectDependenciesFromSelf()) {
            String name = dep.getTargetClass().getName();
            assertThat(name).doesNotContain(".persistence.", ".transaction.", ".integration.");
            assertThat(name).isNotEqualTo(OrderPlacementCoordinator.class.getName());
        }
    }

    static void boundary(JavaClass type, Set<String> allowed, boolean useCase) {
        for (var dep : type.getDirectDependenciesFromSelf()) {
            String name = dep.getTargetClass().getName();
            if (name.contains(".application."))
                assertThat(allowed).as(type.getName() + " -> " + name).contains(name);
            assertThat(name)
                    .doesNotContain(
                            ".api.",
                            ".customer.persistence.",
                            ".product.persistence.",
                            ".customer.domain.",
                            ".product.domain.");
            if (useCase)
                assertThat(name)
                        .doesNotContain(
                                ".persistence.",
                                ".transaction.",
                                ".integration.",
                                "org.springframework.jdbc.",
                                "org.springframework.data.",
                                "org.springframework.transaction.");
        }
    }

    @Test
    void adaptersDependOnlyOnExactApplicationContractsAndProviderInputs() {
        boundary(SLICE.get(OrderWriteAdapter.class), Set.of(OrderWriteStore.class.getName()), false);
        boundary(
                SLICE.get(OrderPlacementCustomerAdapter.class),
                Set.of(
                        OrderPlacementCustomerFacts.class.getName(),
                        CustomerFacts.class.getName(),
                        CustomerResult.class.getName()),
                false);
        boundary(
                SLICE.get(OrderPlacementProductAdapter.class),
                Set.of(
                        OrderPlacementProductFacts.class.getName(),
                        ProductFacts.class.getName(),
                        ProductResult.class.getName()),
                false);
        boundary(
                SLICE.get(SpringOrderPlacementAttempts.class),
                Set.of(
                        OrderPlacementAttempts.class.getName(),
                        PlacementAttempt.class.getName(),
                        PlacementAttempt.Completion.class.getName(),
                        OrderResult.class.getName()),
                false);
    }

    @Test
    void recursiveContractsPermitOnlyExactApprovedSameFeatureSaveException() throws Exception {
        for (Class<?> port : List.of(
                OrderPlacementOperations.class,
                OrderWriteStore.class,
                OrderPlacementCustomerFacts.class,
                OrderPlacementProductFacts.class,
                OrderPlacementAttempts.class,
                ProductFacts.class)) contract(port);
        assertThat(OrderWriteStore.class.getDeclaredMethods()).hasSize(2);
        assertThat(OrderWriteStore.class.getDeclaredMethod("save", Order.class).getReturnType())
                .isEqualTo(Order.class);
        assertThat(OrderWriteStore.class.getDeclaredMethod("flush").getReturnType())
                .isEqualTo(void.class);
    }

    static void contract(Class<?> port) {
        assertThat(port.isInterface()).isTrue();
        for (Method method : port.getMethods()) {
            boolean exception = port == OrderWriteStore.class
                    && method.getName().equals("save")
                    && method.getReturnType() == Order.class
                    && Arrays.equals(method.getParameterTypes(), new Class<?>[] {Order.class});
            if (!exception) {
                neutral(method.getGenericReturnType(), new HashSet<>());
                for (Type parameter : method.getGenericParameterTypes()) neutral(parameter, new HashSet<>());
            }
        }
    }

    static void neutral(Type type, Set<Type> seen) {
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
                    "java.nio.file.")) assertThat(concrete.getName()).doesNotStartWith(prefix);
            if (concrete.isRecord())
                for (RecordComponent component : concrete.getRecordComponents())
                    neutral(component.getGenericType(), seen);
            if (concrete.isArray()) neutral(concrete.getComponentType(), seen);
        } else if (type instanceof WildcardType wildcard) {
            for (Type bound : wildcard.getUpperBounds()) neutral(bound, seen);
            for (Type bound : wildcard.getLowerBounds()) neutral(bound, seen);
        } else if (type instanceof TypeVariable<?> variable) {
            for (Type bound : variable.getBounds()) neutral(bound, seen);
        }
    }

    @Test
    void rejectsEntityLeaksIncludingSaveLookalikesAndNestedPayloads() {
        for (Class<?> type : List.of(EntityLeak.class, SaveLookalike.class, NestedLeak.class, HttpLeak.class))
            assertThatThrownBy(() -> contract(type)).isInstanceOf(AssertionError.class);
    }

    @Test
    void rejectsConcreteControllerUseCaseInfrastructureAndProviderBypasses() {
        var invalid = new ClassFileImporter()
                .importClasses(
                        ControllerBypass.class,
                        UseCaseBypass.class,
                        ProviderBypass.class,
                        AdapterBypass.class,
                        ConcreteProviderBypass.class);
        var controller = invalid.get(ControllerBypass.class);
        assertThatThrownBy(() -> controller(controller)).isInstanceOf(AssertionError.class);
        var useCase = invalid.get(UseCaseBypass.class);
        Set<String> noCapabilities = Set.of();
        assertThatThrownBy(() -> boundary(useCase, noCapabilities, true)).isInstanceOf(AssertionError.class);
        var provider = invalid.get(ProviderBypass.class);
        var providerCapabilities = Set.of(CustomerFacts.class.getName());
        assertThatThrownBy(() -> boundary(provider, providerCapabilities, false))
                .isInstanceOf(AssertionError.class);
        var adapter = invalid.get(AdapterBypass.class);
        var adapterCapabilities = Set.of(OrderWriteStore.class.getName());
        assertThatThrownBy(() -> boundary(adapter, adapterCapabilities, false)).isInstanceOf(AssertionError.class);
        var concreteProvider = invalid.get(ConcreteProviderBypass.class);
        var concreteProviderCapabilities = Set.of(CustomerFacts.class.getName(), CustomerResult.class.getName());
        assertThatThrownBy(() -> boundary(concreteProvider, concreteProviderCapabilities, false))
                .isInstanceOf(AssertionError.class);
    }

    interface EntityLeak {
        Order get();
    }

    interface SaveLookalike {
        Order save(Order order);
    }

    record NestedEntity(Order order) {}

    interface NestedLeak {
        List<NestedEntity> get();
    }

    interface HttpLeak {
        org.springframework.http.ResponseEntity<OrderResult> get();
    }

    static class ControllerBypass {
        OrderPlacementOperations input;
        OrderPlacementCoordinator concrete;

        OrderResult place(PlaceOrderCommand command) {
            input.place(command);
            return concrete.place(command);
        }
    }

    static class UseCaseBypass {
        OrderRepository repository;
        org.springframework.jdbc.core.JdbcTemplate jdbc;
    }

    static class ProviderBypass {
        com.example.agenticorderservice.customer.persistence.CustomerRepository repository;
    }

    static class AdapterBypass {
        OrderPlacementCoordinator coordinator;
    }

    static class ConcreteProviderBypass {
        com.example.agenticorderservice.customer.application.CustomerOrderAccess provider;
    }
}
