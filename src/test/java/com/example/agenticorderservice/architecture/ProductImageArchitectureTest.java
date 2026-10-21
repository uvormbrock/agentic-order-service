package com.example.agenticorderservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.product.api.ProductImageController;
import com.example.agenticorderservice.product.application.*;
import com.example.agenticorderservice.product.application.port.in.ProductImageOperations;
import com.example.agenticorderservice.product.application.port.out.*;
import com.example.agenticorderservice.product.persistence.ProductImageMetadataAdapter;
import com.example.agenticorderservice.product.storage.*;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ProductImageArchitectureTest {
    static final String FEATURE = "com.example.agenticorderservice.product";
    static final Class<?>[] PORTS = {
        ProductImageOperations.class,
        ImageProcessing.class,
        ImageStorage.class,
        ProductImageMetadataStore.class,
        ProductImageTransactions.class
    };
    static final Class<?>[] MANIFEST = {
        ProductImageController.class,
        ProductImageApplicationService.class,
        ProductImageOperations.class,
        ImageProcessing.class,
        ImageStorage.class,
        ProductImageMetadataStore.class,
        ProductImageTransactions.class,
        ProductImageMetadataAdapter.class,
        ProductImageProcessor.class,
        ProductImageStorage.class,
        SpringProductImageTransactions.class,
        ProcessedImage.class,
        StagedImage.class,
        ProductImageSnapshot.class,
        ProductImageContent.class,
        ProductImageLimits.class
    };
    static final JavaClasses SLICE = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importClasses(MANIFEST);

    @Test
    void importsActualProductionScopeAndAllFiveImplementedPorts() {
        for (Class<?> type : MANIFEST) {
            assertThat(SLICE.contain(type)).as(type.getName()).isTrue();
            assertThat(SLICE.get(type).reflect()).isEqualTo(type);
        }
        assertThat(ProductImageOperations.class.isAssignableFrom(ProductImageApplicationService.class))
                .isTrue();
        assertThat(ImageProcessing.class.isAssignableFrom(ProductImageProcessor.class))
                .isTrue();
        assertThat(ImageStorage.class.isAssignableFrom(ProductImageStorage.class))
                .isTrue();
        assertThat(ProductImageMetadataStore.class.isAssignableFrom(ProductImageMetadataAdapter.class))
                .isTrue();
        assertThat(ProductImageTransactions.class.isAssignableFrom(SpringProductImageTransactions.class))
                .isTrue();
        for (Class<?> port : List.of(
                ImageProcessing.class,
                ImageStorage.class,
                ProductImageMetadataStore.class,
                ProductImageTransactions.class)) {
            assertThat(SLICE.get(ProductImageApplicationService.class).getDirectDependenciesFromSelf())
                    .anySatisfy(d -> assertThat(d.getTargetClass().getName()).isEqualTo(port.getName()));
        }
    }

    @Test
    void migratedScopeRespectsDependencyDirectionAndInfrastructureNeutralContracts() {
        HexagonalArchitectureRules.application(FEATURE).check(SLICE);
        HexagonalArchitectureRules.portInterfaces(FEATURE).check(SLICE);
        HexagonalArchitectureRules.portDependencies(FEATURE).check(SLICE);
        HexagonalArchitectureRules.api(FEATURE).check(SLICE);
        HexagonalArchitectureRules.outboundAdaptersDoNotAccessUseCase(FEATURE, ProductImageApplicationService.class)
                .check(SLICE);
        noClasses()
                .that()
                .resideInAPackage(FEATURE + ".application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework.transaction..", "java.awt..")
                .check(SLICE);
        for (Class<?> port : PORTS) assertContract(port);
        for (Class<?> invalid : List.of(EntityLeak.class, NestedEntityLeak.class, HttpLeak.class, FileLeak.class)) {
            assertThatThrownBy(() -> assertContract(invalid))
                    .as(invalid.getName())
                    .isInstanceOf(AssertionError.class);
        }
    }

    @Test
    void everyHttpOperationInvokesOnlyTheApprovedInputPort() {
        assertController(SLICE.get(ProductImageController.class));
        var invalid = new ClassFileImporter().importClasses(ConcreteBypass.class);
        var controller = invalid.get(ConcreteBypass.class);
        assertThatThrownBy(() -> assertController(controller)).isInstanceOf(AssertionError.class);
    }

    static void assertController(JavaClass controller) {
        for (String method : List.of("upload", "original", "thumbnail", "delete")) {
            var matching = controller.getMethods().stream()
                    .filter(m -> m.getName().equals(method))
                    .toList();
            assertThat(matching).hasSize(1);
            assertThat(matching.getFirst().getMethodCallsFromSelf())
                    .anySatisfy(call -> assertThat(call.getTargetOwner().getName())
                            .isEqualTo(ProductImageOperations.class.getName()));
        }
        for (var dependency : controller.getDirectDependenciesFromSelf()) {
            String name = dependency.getTargetClass().getName();
            if (name.startsWith(FEATURE + ".application.")) {
                assertThat(name)
                        .isIn(
                                ProductImageOperations.class.getName(),
                                ProductImageContent.class.getName(),
                                ProductImageFailure.class.getName(),
                                ProductImageFailure.Reason.class.getName(),
                                ProductImageLimits.class.getName());
            }
        }
    }

    @Test
    void detectsRepresentativeApplicationInfrastructureDependencies() {
        for (Class<?> invalid : List.of(
                ProductImageForbiddenDependencies.RepositoryAccess.class,
                ProductImageForbiddenDependencies.FilesystemAccess.class,
                ProductImageForbiddenDependencies.ImageIoAccess.class,
                ProductImageForbiddenDependencies.StorageAccess.class)) {
            var applicationRule = HexagonalArchitectureRules.application(FEATURE);
            var forbiddenClasses = new ClassFileImporter().importClasses(invalid);
            assertThatThrownBy(() -> applicationRule.check(forbiddenClasses))
                    .as(invalid.getName())
                    .isInstanceOf(AssertionError.class);
        }
        var transactionRule = noClasses()
                .that()
                .resideInAPackage(FEATURE + ".application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework.transaction..", "java.awt..");
        var transactionClasses =
                new ClassFileImporter().importClasses(ProductImageForbiddenDependencies.TransactionAccess.class);
        assertThatThrownBy(() -> transactionRule.check(transactionClasses)).isInstanceOf(AssertionError.class);
    }

    @Test
    void detectsOutboundAdapterCallingTheConcreteUseCase() {
        var adapterRule = HexagonalArchitectureRules.outboundAdaptersDoNotAccessUseCase(
                FEATURE, ProductImageApplicationService.class);
        var bypassClasses = new ClassFileImporter().importClasses(ProductImageUseCaseBypass.class);
        assertThatThrownBy(() -> adapterRule.check(bypassClasses)).isInstanceOf(AssertionError.class);
        var awtRule = noClasses()
                .that()
                .resideInAPackage(FEATURE + ".application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework.transaction..", "java.awt..");
        var awtClasses = new ClassFileImporter().importClasses(ProductImageForbiddenDependencies.AwtAccess.class);
        assertThatThrownBy(() -> awtRule.check(awtClasses)).isInstanceOf(AssertionError.class);
    }

    static void assertContract(Class<?> port) {
        assertThat(port.isInterface()).isTrue();
        for (Method method : port.getMethods()) {
            assertType(method.getGenericReturnType(), new HashSet<>());
            for (Type type : method.getGenericParameterTypes()) assertType(type, new HashSet<>());
        }
    }

    static void assertType(Type type, Set<Type> visited) {
        if (!visited.add(type)) return;
        if (type instanceof ParameterizedType generic) {
            assertType(generic.getRawType(), visited);
            for (Type argument : generic.getActualTypeArguments()) assertType(argument, visited);
        } else if (type instanceof Class<?> concrete) {
            assertThat(concrete.isAnnotationPresent(jakarta.persistence.Entity.class))
                    .isFalse();
            for (String prefix : List.of(
                    "org.springframework.",
                    "jakarta.persistence.",
                    "jakarta.servlet.",
                    "org.hibernate.",
                    "java.awt.",
                    "java.io.",
                    "java.nio.file.",
                    "javax.imageio.")) {
                assertThat(concrete.getName()).doesNotStartWith(prefix);
            }
            if (concrete.isArray()) assertType(concrete.getComponentType(), visited);
            if (concrete.isRecord())
                for (RecordComponent component : concrete.getRecordComponents())
                    assertType(component.getGenericType(), visited);
        } else if (type instanceof TypeVariable<?> variable) {
            for (Type bound : variable.getBounds()) assertType(bound, visited);
        } else if (type instanceof WildcardType wildcard) {
            for (Type bound : wildcard.getUpperBounds()) assertType(bound, visited);
            for (Type bound : wildcard.getLowerBounds()) assertType(bound, visited);
        } else if (type instanceof GenericArrayType array) assertType(array.getGenericComponentType(), visited);
    }

    interface EntityLeak {
        com.example.agenticorderservice.product.domain.ProductImage find();
    }

    record EntityEnvelope(List<com.example.agenticorderservice.product.domain.ProductImage> images) {}

    interface NestedEntityLeak {
        EntityEnvelope find();
    }

    interface HttpLeak {
        org.springframework.http.ResponseEntity<byte[]> get();
    }

    interface FileLeak {
        java.nio.file.Path stage();
    }

    static class ConcreteBypass {
        ProductImageOperations input;
        ProductImageApplicationService concrete;

        void upload(UUID id, byte[] bytes, String media) {
            input.upload(id, bytes, media);
            concrete.upload(id, bytes, media);
        }

        ProductImageContent original(UUID id) {
            return input.get(id, false);
        }

        ProductImageContent thumbnail(UUID id) {
            return input.get(id, true);
        }

        void delete(UUID id) {
            input.delete(id);
        }
    }
}
