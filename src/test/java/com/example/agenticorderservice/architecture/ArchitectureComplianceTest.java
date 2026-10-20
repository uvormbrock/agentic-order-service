package com.example.agenticorderservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Static boundaries from system-architecture.md and Accepted ADR-0003; JPA mapping is intentional. */
class ArchitectureComplianceTest {
    private static final String ROOT = "com.example.agenticorderservice";
    private static final JavaClasses PRODUCTION = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void domainDependsOnlyOnDomainJavaAndJpaMetadata() {
        noClasses()
                .that()
                .resideInAPackage(ROOT + "..domain..")
                .should()
                .dependOnClassesThat()
                .resideOutsideOfPackages(ROOT + "..domain..", "java..", "jakarta.persistence..")
                .because("direct JPA mapping is accepted, but domain behavior must not call outer layers")
                .check(PRODUCTION);
    }

    @Test
    void applicationDoesNotDependOnHttpAdapters() {
        noClasses()
                .that()
                .resideInAPackage(ROOT + "..application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        ROOT + "..api..",
                        "org.springframework.web..",
                        "org.springframework.http..",
                        "jakarta.servlet..")
                .because("application operations return transport-independent results")
                .check(PRODUCTION);
    }

    // REST error mapping may inspect provider exceptions; this rule targets component access.
    @Test
    void apiDelegatesWithoutDomainOrPersistenceAccess() {
        noClasses()
                .that()
                .resideInAPackage(ROOT + "..api..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        ROOT + "..domain..",
                        ROOT + "..persistence..",
                        ROOT + "..storage..",
                        "org.springframework.data..",
                        "org.springframework.jdbc..")
                .because("REST adapters delegate to application operations rather than repositories or aggregates")
                .check(PRODUCTION);
    }

    @Test
    void persistenceDoesNotDependOnApiOrApplication() {
        noClasses()
                .that()
                .resideInAPackage(ROOT + "..persistence..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(ROOT + "..api..", ROOT + "..application..")
                .because("persistence must not orchestrate use cases or depend on transport adapters")
                .check(PRODUCTION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"customer", "product", "order"})
    void featureInternalsAreNotAccessedByOtherFeatures(String feature) {
        for (String other : new String[] {"customer", "product", "order"}) {
            if (!feature.equals(other)) {
                noClasses()
                        .that()
                        .resideInAPackage(ROOT + "." + feature + "..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(
                                ROOT + "." + other + ".domain..", ROOT + "." + other + ".persistence..",
                                ROOT + "." + other + ".api..", ROOT + "." + other + ".storage..")
                        .because(
                                "cross-feature access uses provider-owned application contracts, never foreign internals")
                        .check(PRODUCTION);
            }
        }
    }

    @Test
    void featureAndSharedCodeDoNotDependOnCompositionRoot() {
        noClasses()
                .that()
                .resideInAnyPackage(ROOT + ".customer..", ROOT + ".product..", ROOT + ".order..", ROOT + ".shared..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(ROOT + ".configuration..")
                .because("configuration wires components; feature behavior must not depend on concrete configuration")
                .check(PRODUCTION);
    }
}
