package com.example.agenticorderservice.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import architecturefixtures.hexagonal.api.ApiFixtures;
import architecturefixtures.hexagonal.application.ApplicationFixtures;
import architecturefixtures.hexagonal.application.port.in.InputPort;
import architecturefixtures.hexagonal.application.port.out.OutputFixtures;
import architecturefixtures.hexagonal.domain.DomainFixtures;
import architecturefixtures.hexagonal.persistence.PersistenceFixtures;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Positive, negative and empty-scope checks for the active migration enforcement rules. */
class HexagonalArchitectureRulesTest {
    private static final String FEATURE = "architecturefixtures.hexagonal";

    static Stream<ArchRule> rules() {
        return Stream.of(
                HexagonalArchitectureRules.domain(FEATURE),
                HexagonalArchitectureRules.application(FEATURE),
                HexagonalArchitectureRules.portInterfaces(FEATURE),
                HexagonalArchitectureRules.portDependencies(FEATURE),
                HexagonalArchitectureRules.api(FEATURE),
                HexagonalArchitectureRules.outboundAdaptersDoNotAccessUseCase(
                        FEATURE, ApplicationFixtures.UseCase.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"customer", "product", "order", "shared"})
    void productionDomainRetainsOnlyTheApprovedJpaMetadataException(String feature) {
        String root = "com.example.agenticorderservice." + feature;
        JavaClasses production = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(root + ".domain");
        HexagonalArchitectureRules.domain(root).check(production);
    }

    @ParameterizedTest
    @MethodSource("rules")
    void acceptsPortsAdaptersAndTheApprovedJpaMetadataException(ArchRule rule) {
        JavaClasses valid = new ClassFileImporter()
                .importClasses(
                        DomainFixtures.Aggregate.class, ApplicationFixtures.UseCase.class,
                        InputPort.class, OutputFixtures.OutputPort.class,
                        ApiFixtures.Controller.class, PersistenceFixtures.Adapter.class);
        rule.check(valid);
    }

    static Stream<Arguments> violations() {
        return Stream.of(
                Arguments.of(
                        HexagonalArchitectureRules.domain(FEATURE),
                        DomainFixtures.InvalidJpaRuntime.class,
                        jakarta.persistence.EntityManager.class),
                Arguments.of(
                        HexagonalArchitectureRules.domain(FEATURE),
                        DomainFixtures.InvalidDomain.class,
                        ApplicationFixtures.UseCase.class),
                Arguments.of(
                        HexagonalArchitectureRules.application(FEATURE),
                        ApplicationFixtures.InvalidRepositoryAccess.class,
                        PersistenceFixtures.Adapter.class),
                Arguments.of(
                        HexagonalArchitectureRules.application(FEATURE),
                        ApplicationFixtures.InvalidIntegrationAccess.class,
                        architecturefixtures.hexagonal.integration.IntegrationFixtures.class),
                Arguments.of(
                        HexagonalArchitectureRules.application(FEATURE),
                        ApplicationFixtures.InvalidTransactionAdapterAccess.class,
                        architecturefixtures.hexagonal.transaction.TransactionFixtures.class),
                Arguments.of(
                        HexagonalArchitectureRules.application(FEATURE),
                        ApplicationFixtures.InvalidFilesystemAccess.class,
                        java.nio.file.Path.class),
                Arguments.of(
                        HexagonalArchitectureRules.portInterfaces(FEATURE),
                        OutputFixtures.InvalidConcretePort.class,
                        OutputFixtures.InvalidConcretePort.class),
                Arguments.of(
                        HexagonalArchitectureRules.portDependencies(FEATURE),
                        OutputFixtures.InvalidEntityPort.class,
                        DomainFixtures.Aggregate.class),
                Arguments.of(
                        HexagonalArchitectureRules.portDependencies(FEATURE),
                        OutputFixtures.InvalidHttpPort.class,
                        org.springframework.http.ResponseEntity.class),
                Arguments.of(
                        HexagonalArchitectureRules.portDependencies(FEATURE),
                        OutputFixtures.InvalidRepositoryPort.class,
                        org.springframework.data.repository.Repository.class),
                Arguments.of(
                        HexagonalArchitectureRules.api(FEATURE),
                        ApiFixtures.InvalidRepositoryAccess.class,
                        PersistenceFixtures.Adapter.class),
                Arguments.of(
                        HexagonalArchitectureRules.api(FEATURE),
                        ApiFixtures.InvalidEntityAccess.class,
                        DomainFixtures.Aggregate.class),
                Arguments.of(
                        HexagonalArchitectureRules.outboundAdaptersDoNotAccessUseCase(
                                FEATURE, ApplicationFixtures.UseCase.class),
                        PersistenceFixtures.InvalidUseCaseAccess.class,
                        ApplicationFixtures.UseCase.class));
    }

    @ParameterizedTest
    @MethodSource("violations")
    void rejectsForbiddenDependenciesWithActionableClassNames(ArchRule rule, Class<?> origin, Class<?> target) {
        JavaClasses invalid = new ClassFileImporter().importClasses(origin);
        assertThat(rule.evaluate(invalid).hasViolation()).isTrue();
        assertThatThrownBy(() -> rule.check(invalid))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(origin.getName())
                .hasMessageContaining(target.getName());
    }

    @ParameterizedTest
    @MethodSource("rules")
    void refusesAnEmptyOrMisspelledMigrationScope(ArchRule rule) {
        JavaClasses unrelated = new ClassFileImporter().importClasses(String.class);
        assertThatThrownBy(() -> rule.check(unrelated)).isInstanceOf(AssertionError.class);
    }
}
