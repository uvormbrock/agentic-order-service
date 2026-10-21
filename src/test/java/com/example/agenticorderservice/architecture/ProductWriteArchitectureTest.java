package com.example.agenticorderservice.architecture;

import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.product.api.*;
import com.example.agenticorderservice.product.application.*;
import com.example.agenticorderservice.product.application.port.in.ProductWriteOperations;
import com.example.agenticorderservice.product.application.port.out.ProductWriteStore;
import com.example.agenticorderservice.product.domain.Product;
import com.example.agenticorderservice.product.persistence.*;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Production-facing enforcement of the approved Product write slice and exact aggregate exception. */
class ProductWriteArchitectureTest {
    private static final String FEATURE = "com.example.agenticorderservice.product";
    private static final Class<?>[] MANIFEST = {
        ProductController.class, ProductWriteOperations.class, ProductWriteStore.class,
        ProductWriteService.class, ProductWriteAdapter.class, ProductResult.class
    };
    private static final JavaClasses SLICE = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importClasses(MANIFEST);

    @Test
    void importsApprovedProductionManifestAndChecksBothPortImplementations() {
        for (Class<?> type : MANIFEST) {
            assertThat(SLICE.contain(type)).as(type.getName()).isTrue();
            assertThat(SLICE.get(type).reflect()).isEqualTo(type);
        }
        assertThat(ProductWriteOperations.class.isAssignableFrom(ProductWriteService.class))
                .isTrue();
        assertThat(ProductWriteStore.class.isAssignableFrom(ProductWriteAdapter.class))
                .isTrue();
        assertThat(SLICE.get(ProductWriteService.class).getDirectDependenciesFromSelf())
                .anySatisfy(dependency ->
                        assertThat(dependency.getTargetClass().reflect()).isEqualTo(ProductWriteStore.class));
    }

    @Test
    void migratedApplicationAndAdaptersRespectInwardDependencies() {
        HexagonalArchitectureRules.application(FEATURE).check(SLICE);
        HexagonalArchitectureRules.portInterfaces(FEATURE).check(SLICE);
        HexagonalArchitectureRules.api(FEATURE).check(SLICE);
        HexagonalArchitectureRules.outboundAdaptersDoNotAccessUseCase(FEATURE, ProductWriteService.class)
                .check(SLICE);
        // Keep the general entity prohibition intact; the single approved output port is checked separately below.
        var ordinaryPorts = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importClasses(ProductWriteOperations.class);
        HexagonalArchitectureRules.portDependencies(FEATURE).check(ordinaryPorts);
        for (var dependency : SLICE.get(ProductWriteStore.class).getDirectDependenciesFromSelf()) {
            String target = dependency.getTargetClass().getName();
            assertThat(target.startsWith("java.") || target.equals(Product.class.getName()))
                    .as("Exact ProductWriteStore/Product exception: %s", target)
                    .isTrue();
        }
    }

    @Test
    void onlyExactApprovedOutputPortMayExposeProductAggregate() {
        assertContract(ProductWriteOperations.class);
        assertContract(ProductWriteStore.class);
        for (Class<?> invalid :
                List.of(ProductInputLeak.class, OtherProductOutputLeak.class, OtherEntityLeak.class, HttpLeak.class)) {
            assertThatThrownBy(() -> assertContract(invalid))
                    .as(invalid.getName())
                    .isInstanceOf(AssertionError.class);
        }
        // The shared rule must continue rejecting entities for ordinary output ports.
        var portRule = HexagonalArchitectureRules.portDependencies(FEATURE);
        assertThatThrownBy(() -> portRule.check(SLICE)).isInstanceOf(AssertionError.class);
    }

    @Test
    void outputAggregateExceptionRetainsOnlyApprovedCapabilities() throws Exception {
        assertThat(ProductWriteStore.class.getMethods()).hasSize(2);
        assertThat(ProductWriteStore.class.getMethod("save", Product.class).getReturnType())
                .isEqualTo(Product.class);
        assertThat(ProductWriteStore.class
                        .getMethod("findLockedById", UUID.class)
                        .getGenericReturnType()
                        .getTypeName())
                .isEqualTo("java.util.Optional<" + Product.class.getName() + ">");
        assertThat(ProductWriteOperations.class.getMethods()).hasSize(3);
        assertThat(ProductWriteOperations.class
                        .getMethod("create", String.class, BigDecimal.class)
                        .getReturnType())
                .isEqualTo(ProductResult.class);
        assertThat(ProductWriteOperations.class
                        .getMethod("update", UUID.class, String.class, BigDecimal.class)
                        .getReturnType())
                .isEqualTo(ProductResult.class);
        assertThat(ProductWriteOperations.class
                        .getMethod("deactivate", UUID.class)
                        .getReturnType())
                .isEqualTo(ProductResult.class);
    }

    private static void assertContract(Class<?> port) {
        assertThat(port.isInterface()).isTrue();
        for (Method method : port.getMethods()) {
            assertType(method.getGenericReturnType(), port, new HashSet<>());
            for (Type parameter : method.getGenericParameterTypes()) assertType(parameter, port, new HashSet<>());
        }
    }

    private static void assertType(Type type, Class<?> port, Set<Type> visited) {
        if (!visited.add(type)) return;
        if (type instanceof ParameterizedType generic) {
            assertType(generic.getRawType(), port, visited);
            for (Type argument : generic.getActualTypeArguments()) assertType(argument, port, visited);
        } else if (type instanceof Class<?> concrete) {
            boolean approvedAggregate = port == ProductWriteStore.class && concrete == Product.class;
            assertThat(!concrete.isAnnotationPresent(jakarta.persistence.Entity.class) || approvedAggregate)
                    .as("Managed entity exposure from %s: %s", port, concrete)
                    .isTrue();
            for (String forbidden :
                    List.of("org.springframework.", "jakarta.persistence.", "jakarta.servlet.", "org.hibernate.")) {
                assertThat(concrete.getName()).doesNotStartWith(forbidden);
            }
            if (concrete.isArray()) assertType(concrete.getComponentType(), port, visited);
            if (concrete.isRecord()) {
                for (RecordComponent component : concrete.getRecordComponents())
                    assertType(component.getGenericType(), port, visited);
            }
        } else if (type instanceof WildcardType wildcard) {
            for (Type bound : wildcard.getUpperBounds()) assertType(bound, port, visited);
            for (Type bound : wildcard.getLowerBounds()) assertType(bound, port, visited);
        } else if (type instanceof TypeVariable<?> variable) {
            for (Type bound : variable.getBounds()) assertType(bound, port, visited);
        } else if (type instanceof GenericArrayType array) assertType(array.getGenericComponentType(), port, visited);
    }

    @Test
    void allThreeWriteMethodsDelegateOnlyThroughInputPort() {
        assertWriteBoundary(SLICE.get(ProductController.class));
    }

    private static void assertWriteBoundary(JavaClass controller) {
        for (String methodName : List.of("create", "update", "deactivate")) {
            var methods = controller.getMethods().stream()
                    .filter(method -> method.getName().equals(methodName))
                    .toList();
            assertThat(methods).hasSize(1);
            var method = methods.getFirst();
            assertThat(method.getMethodCallsFromSelf()).anySatisfy(call -> {
                assertThat(call.getTargetOwner().getName()).isEqualTo(ProductWriteOperations.class.getName());
                assertThat(call.getTarget().getName()).isEqualTo(methodName);
            });
            for (var access : method.getAccessesFromSelf()) {
                assertThat(access.getTargetOwner().getName())
                        .isNotIn(
                                ProductApplicationService.class.getName(),
                                ProductWriteService.class.getName(),
                                ProductRepository.class.getName(),
                                ProductWriteAdapter.class.getName(),
                                ProductWriteStore.class.getName());
            }
        }
        for (var dependency : controller.getDirectDependenciesFromSelf()) {
            assertThat(dependency.getTargetClass().getName())
                    .isNotIn(
                            ProductWriteService.class.getName(),
                            ProductWriteAdapter.class.getName(),
                            ProductRepository.class.getName(),
                            ProductWriteStore.class.getName());
        }
    }

    @Test
    void detectsConcreteLegacyAndRepositoryBypassesAlongsideValidPortCalls() {
        for (Class<?> invalid : List.of(LegacyBypass.class, ConcreteBypass.class, RepositoryBypass.class)) {
            var fixture = new ClassFileImporter().importClasses(invalid);
            var bypass = fixture.get(invalid);
            assertThatThrownBy(() -> assertWriteBoundary(bypass))
                    .as(invalid.getName())
                    .isInstanceOf(AssertionError.class);
        }
    }

    interface ProductInputLeak {
        Product create();
    }

    interface OtherProductOutputLeak {
        Optional<Product> find();
    }

    interface OtherEntityLeak {
        com.example.agenticorderservice.customer.domain.Customer find();
    }

    interface HttpLeak {
        org.springframework.http.ResponseEntity<ProductResult> create();
    }

    static class LegacyBypass {
        ProductWriteOperations input;
        ProductApplicationService legacy;

        ProductResult create(String name, BigDecimal price) {
            input.create(name, price);
            return legacy.create(name, price);
        }

        ProductResult update(UUID id, String name, BigDecimal price) {
            return input.update(id, name, price);
        }

        ProductResult deactivate(UUID id) {
            return input.deactivate(id);
        }
    }

    static class ConcreteBypass {
        ProductWriteOperations input;
        ProductWriteService concrete;

        ProductResult create(String name, BigDecimal price) {
            return input.create(name, price);
        }

        ProductResult update(UUID id, String name, BigDecimal price) {
            input.update(id, name, price);
            return concrete.update(id, name, price);
        }

        ProductResult deactivate(UUID id) {
            return input.deactivate(id);
        }
    }

    static class RepositoryBypass {
        ProductWriteOperations input;
        ProductRepository repository;

        ProductResult create(String name, BigDecimal price) {
            return input.create(name, price);
        }

        ProductResult update(UUID id, String name, BigDecimal price) {
            return input.update(id, name, price);
        }

        ProductResult deactivate(UUID id) {
            repository.findLockedById(id);
            return input.deactivate(id);
        }
    }
}
