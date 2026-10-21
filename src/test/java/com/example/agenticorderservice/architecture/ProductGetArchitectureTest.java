package com.example.agenticorderservice.architecture;

import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.product.api.ProductController;
import com.example.agenticorderservice.product.application.*;
import com.example.agenticorderservice.product.application.port.in.GetProductQuery;
import com.example.agenticorderservice.product.application.port.out.FindProductById;
import com.example.agenticorderservice.product.persistence.ProductReadAdapter;
import com.example.agenticorderservice.product.persistence.ProductRepository;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Enforcement is deliberately limited to the approved GET-by-ID migration. */
class ProductGetArchitectureTest {
    private static final String FEATURE = "com.example.agenticorderservice.product";
    private static final Class<?>[] MANIFEST = {
        ProductController.class, GetProductQuery.class, FindProductById.class,
        GetProductService.class, ProductReadAdapter.class, ProductResult.class
    };
    private static final JavaClasses SLICE = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importClasses(MANIFEST);

    @Test
    void importsTheWholeApprovedSliceAndRealDependencyTargets() {
        for (Class<?> type : MANIFEST) {
            assertThat(SLICE.contain(type)).as(type.getName()).isTrue();
            assertThat(SLICE.get(type).reflect()).isEqualTo(type);
        }
        assertThat(SLICE.get(GetProductService.class).getDirectDependenciesFromSelf())
                .anySatisfy(dependency ->
                        assertThat(dependency.getTargetClass().reflect()).isEqualTo(FindProductById.class));
        assertThat(FindProductById.class.isAssignableFrom(ProductReadAdapter.class))
                .isTrue();
        assertThat(GetProductQuery.class.isAssignableFrom(GetProductService.class))
                .isTrue();
    }

    @Test
    void migratedApplicationUsesPortsAndAdaptersDoNotCallUseCase() {
        HexagonalArchitectureRules.application(FEATURE).check(SLICE);
        HexagonalArchitectureRules.portInterfaces(FEATURE).check(SLICE);
        HexagonalArchitectureRules.portDependencies(FEATURE).check(SLICE);
        HexagonalArchitectureRules.api(FEATURE).check(SLICE);
        HexagonalArchitectureRules.outboundAdaptersDoNotAccessUseCase(FEATURE, GetProductService.class)
                .check(SLICE);
    }

    @Test
    void publicPortSignaturesContainOnlyMaterializedTransportNeutralFacts() {
        assertContract(GetProductQuery.class);
        assertContract(FindProductById.class);
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
                    .as("No managed entity in port signature: %s", concrete)
                    .isFalse();
            for (String forbidden :
                    List.of("org.springframework.", "jakarta.persistence.", "jakarta.servlet.", "org.hibernate.")) {
                assertThat(concrete.getName()).doesNotStartWith(forbidden);
            }
            if (concrete.isArray()) assertType(concrete.getComponentType(), visited);
            if (concrete.isRecord()) {
                for (RecordComponent component : concrete.getRecordComponents())
                    assertType(component.getGenericType(), visited);
            }
        } else if (type instanceof WildcardType wildcard) {
            for (Type bound : wildcard.getUpperBounds()) assertType(bound, visited);
            for (Type bound : wildcard.getLowerBounds()) assertType(bound, visited);
        } else if (type instanceof TypeVariable<?> variable) {
            for (Type bound : variable.getBounds()) assertType(bound, visited);
        } else if (type instanceof GenericArrayType array) assertType(array.getGenericComponentType(), visited);
    }

    @Test
    void controllerGetUsesOnlyItsInputPort() {
        assertGetBoundary(SLICE.get(ProductController.class), GetProductQuery.class);
    }

    private static void assertGetBoundary(JavaClass controller, Class<?> inputPort) {
        var get = controller.getMethod("get", UUID.class);
        assertThat(get.getMethodCallsFromSelf()).anySatisfy(call -> {
            assertThat(call.getTargetOwner().getName()).isEqualTo(inputPort.getName());
            assertThat(call.getTarget().getName()).isEqualTo("get");
        });
        for (var access : get.getAccessesFromSelf()) {
            String owner = access.getTargetOwner().getName();
            assertThat(owner)
                    .isNotEqualTo(ProductApplicationService.class.getName())
                    .isNotEqualTo(GetProductService.class.getName())
                    .isNotEqualTo(ProductRepository.class.getName())
                    .isNotEqualTo(ProductReadAdapter.class.getName());
        }
        for (var dependency : controller.getDirectDependenciesFromSelf()) {
            assertThat(dependency.getTargetClass().getName())
                    .isNotEqualTo(GetProductService.class.getName())
                    .isNotEqualTo(ProductReadAdapter.class.getName());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejectsManagedEntityAndHttpContractLeaks(boolean entity) {
        assertThatThrownBy(() -> assertContract(entity ? EntityLeak.class : HttpLeak.class))
                .isInstanceOf(AssertionError.class);
    }

    interface EntityLeak {
        Optional<com.example.agenticorderservice.product.domain.Product> get(UUID id);
    }

    interface HttpLeak {
        org.springframework.http.ResponseEntity<ProductResult> get(UUID id);
    }

    @Test
    void detectsLegacyRepositoryAndConcreteUseCaseBypassesEvenAlongsidePortCall() {
        for (Class<?> invalid : List.of(LegacyBypass.class, RepositoryBypass.class, UseCaseBypass.class)) {
            var imported = new ClassFileImporter().importClasses(invalid);
            assertThatThrownBy(() -> assertGetBoundary(imported.get(invalid), GetProductQuery.class))
                    .isInstanceOf(AssertionError.class);
        }
    }

    static class LegacyBypass {
        GetProductQuery input;
        ProductApplicationService legacy;

        public ProductResult get(UUID id) {
            input.get(id);
            return legacy.get(id);
        }
    }

    static class RepositoryBypass {
        GetProductQuery input;
        ProductRepository repository;

        public ProductResult get(UUID id) {
            repository.findById(id);
            return input.get(id);
        }
    }

    static class UseCaseBypass {
        GetProductQuery input;
        GetProductService concrete;

        public ProductResult get(UUID id) {
            input.get(id);
            return concrete.get(id);
        }
    }

    @Test
    void missingMigrationPackagesCannotPass() {
        var empty = new ClassFileImporter().importClasses(String.class);
        assertThatThrownBy(() -> HexagonalArchitectureRules.application(FEATURE).check(empty))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(
                        () -> HexagonalArchitectureRules.portInterfaces(FEATURE).check(empty))
                .isInstanceOf(AssertionError.class);
    }
}
