package com.example.agenticorderservice.architecture;

import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.customer.api.CustomerController;
import com.example.agenticorderservice.customer.application.*;
import com.example.agenticorderservice.customer.application.port.in.*;
import com.example.agenticorderservice.customer.application.port.out.*;
import com.example.agenticorderservice.customer.domain.Customer;
import com.example.agenticorderservice.customer.persistence.*;
import com.example.agenticorderservice.order.application.port.in.OrderReferences;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.*;
import org.junit.jupiter.api.Test;

class CustomerArchitectureTest {
    private static final String FEATURE = "com.example.agenticorderservice.customer";
    private static final Class<?>[] MANIFEST = {
        CustomerController.class,
        CustomerApplicationService.class,
        CustomerOrderAccess.class,
        CustomerOperations.class,
        CustomerFacts.class,
        CustomerReadStore.class,
        CustomerWriteStore.class,
        CustomerOrderReferences.class,
        CustomerReadAdapter.class,
        CustomerWriteAdapter.class,
        CustomerOrderReferenceAdapter.class,
        CustomerResult.class,
        Customer.class
    };

    @Test
    void populatedCustomerSliceUsesInwardDependenciesAndImplementedContracts() {
        var slice = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importClasses(MANIFEST);
        for (Class<?> type : MANIFEST)
            assertThat(slice.contain(type)).as(type.getName()).isTrue();
        HexagonalArchitectureRules.domain(FEATURE).check(slice);
        HexagonalArchitectureRules.application(FEATURE).check(slice);
        HexagonalArchitectureRules.api(FEATURE).check(slice);
        HexagonalArchitectureRules.portInterfaces(FEATURE).check(slice);
        HexagonalArchitectureRules.outboundAdaptersDoNotAccessUseCase(FEATURE, CustomerApplicationService.class)
                .check(slice);
        assertThat(CustomerOperations.class.isAssignableFrom(CustomerApplicationService.class))
                .isTrue();
        assertThat(CustomerFacts.class.isAssignableFrom(CustomerOrderAccess.class))
                .isTrue();
        assertThat(CustomerReadStore.class.isAssignableFrom(CustomerReadAdapter.class))
                .isTrue();
        assertThat(CustomerWriteStore.class.isAssignableFrom(CustomerWriteAdapter.class))
                .isTrue();
        assertThat(CustomerOrderReferences.class.isAssignableFrom(CustomerOrderReferenceAdapter.class))
                .isTrue();
        var ordinary = new ClassFileImporter()
                .importClasses(
                        CustomerOperations.class,
                        CustomerFacts.class,
                        CustomerReadStore.class,
                        CustomerOrderReferences.class);
        HexagonalArchitectureRules.portDependencies(FEATURE).check(ordinary);
        var portRule = HexagonalArchitectureRules.portDependencies(FEATURE);
        assertThatThrownBy(() -> portRule.check(slice)).isInstanceOf(AssertionError.class);
    }

    @Test
    void exactAggregateExceptionAndCapabilitiesRemainBounded() throws Exception {
        assertThat(CustomerWriteStore.class.getDeclaredMethods()).hasSize(3);
        assertThat(CustomerWriteStore.class.getMethod("save", Customer.class).getReturnType())
                .isEqualTo(Customer.class);
        assertThat(CustomerWriteStore.class.getMethod("delete", Customer.class).getReturnType())
                .isEqualTo(void.class);
        assertThat(CustomerWriteStore.class
                        .getMethod("findLockedById", UUID.class)
                        .getGenericReturnType()
                        .getTypeName())
                .isEqualTo("java.util.Optional<" + Customer.class.getName() + ">");
        var port =
                new ClassFileImporter().importClasses(CustomerWriteStore.class).get(CustomerWriteStore.class);
        for (var dependency : port.getDirectDependenciesFromSelf()) {
            String target = dependency.getTargetClass().getName();
            assertThat(target.startsWith("java.") || target.equals(Customer.class.getName()))
                    .as(target)
                    .isTrue();
        }
    }

    @Test
    void everyControllerOperationCallsInputPortAndCannotBypassIt() {
        var controller =
                new ClassFileImporter().importClasses(CustomerController.class).get(CustomerController.class);
        for (String name : List.of("create", "get", "list", "update", "deactivate", "delete")) {
            var methods = controller.getMethods().stream()
                    .filter(method -> method.getName().equals(name))
                    .toList();
            assertThat(methods).hasSize(1);
            assertThat(methods.getFirst().getMethodCallsFromSelf()).anySatisfy(call -> {
                assertThat(call.getTargetOwner().getName()).isEqualTo(CustomerOperations.class.getName());
                assertThat(call.getTarget().getName()).isEqualTo(name);
            });
        }
        assertNoControllerBypass(controller);
    }

    private static void assertNoControllerBypass(com.tngtech.archunit.core.domain.JavaClass controller) {
        assertThat(controller.getDirectDependenciesFromSelf())
                .noneSatisfy(
                        dependency -> assertThat(dependency.getTargetClass().getName())
                                .isIn(
                                        CustomerApplicationService.class.getName(),
                                        CustomerRepository.class.getName(),
                                        CustomerReadAdapter.class.getName(),
                                        CustomerWriteAdapter.class.getName()));
    }

    @Test
    void concreteControllerBypassIsRejectedEvenWhenInputPortIsPresent() {
        var fixture =
                new ClassFileImporter().importClasses(InvalidController.class).get(InvalidController.class);
        assertThatThrownBy(() -> assertNoControllerBypass(fixture)).isInstanceOf(AssertionError.class);
    }

    static class InvalidController {
        CustomerOperations input;
        CustomerApplicationService forbidden;
    }

    @Test
    void customerRulesRejectApplicationRepositoryAccessAndUnapprovedEntityPort() {
        var application = new ClassFileImporter().importClasses(CustomerForbiddenDependencyFixture.class);
        var applicationRule = HexagonalArchitectureRules.application(FEATURE);
        assertThatThrownBy(() -> applicationRule.check(application)).isInstanceOf(AssertionError.class);
        var port = new ClassFileImporter().importClasses(CustomerEntityLeakFixture.class);
        var portRule = HexagonalArchitectureRules.portDependencies(FEATURE);
        assertThatThrownBy(() -> portRule.check(port)).isInstanceOf(AssertionError.class);
        var repository = new ClassFileImporter()
                .importClasses(RepositoryController.class)
                .get(RepositoryController.class);
        assertThatThrownBy(() -> assertNoControllerBypass(repository)).isInstanceOf(AssertionError.class);
    }

    static class RepositoryController {
        CustomerOperations input;
        CustomerRepository forbidden;
    }

    @Test
    void adaptersOnlyDependOnExactApprovedApplicationContracts() {
        assertAdapter(
                CustomerReadAdapter.class,
                Set.of(CustomerReadStore.class, CustomerResult.class, CustomerNotFoundException.class));
        assertAdapter(CustomerWriteAdapter.class, Set.of(CustomerWriteStore.class));
        assertAdapter(
                CustomerOrderReferenceAdapter.class, Set.of(CustomerOrderReferences.class, OrderReferences.class));
        Set<Class<?>> allowedDependencies = Set.of(CustomerReadStore.class);
        assertThatThrownBy(() -> assertAdapter(InvalidAdapter.class, allowedDependencies))
                .isInstanceOf(AssertionError.class);
    }

    private static void assertAdapter(Class<?> adapter, Set<Class<?>> allowed) {
        var imported = new ClassFileImporter().importClasses(adapter).get(adapter);
        for (var dependency : imported.getDirectDependenciesFromSelf()) {
            String target = dependency.getTargetClass().getName();
            if (target.contains(".application."))
                assertThat(allowed).contains(dependency.getTargetClass().reflect());
            assertThat(target).doesNotContain(".api.");
        }
    }

    static class InvalidAdapter {
        CustomerApplicationService forbidden;
    }
}
