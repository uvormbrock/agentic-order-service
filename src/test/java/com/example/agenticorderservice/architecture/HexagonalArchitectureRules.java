package com.example.agenticorderservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

/** ADR-0007 Option B rules, to be activated for explicitly migrated slices. */
final class HexagonalArchitectureRules {
    private HexagonalArchitectureRules() {}

    static ArchRule domain(String feature) {
        return noClasses()
                .that()
                .resideInAPackage(feature + ".domain..")
                .should()
                .dependOnClassesThat(JavaClass.Predicates.resideOutsideOfPackages(
                                feature + ".domain..",
                                "com.example.agenticorderservice.shared.domain..",
                                "java..",
                                "jakarta.persistence..")
                        .or(JavaClass.Predicates.resideInAPackage("jakarta.persistence..")
                                .and(DescribedPredicate.describe(
                                        "JPA runtime types", type -> !type.isAnnotation() && !type.isEnum()))))
                .because("Option B permits JPA metadata, not application or adapter dependencies");
    }

    static ArchRule application(String feature) {
        return noClasses()
                .that()
                .resideInAPackage(feature + ".application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        feature + ".api..",
                        feature + ".persistence..",
                        feature + ".storage..",
                        feature + ".integration..",
                        feature + ".transaction..",
                        "org.springframework.web..",
                        "org.springframework.http..",
                        "jakarta.servlet..",
                        "org.springframework.data..",
                        "org.springframework.jdbc..",
                        "jakarta.persistence..",
                        "org.hibernate..",
                        "java.sql..",
                        "javax.sql..",
                        "java.io..",
                        "java.nio.file..",
                        "javax.imageio..")
                .because("migrated orchestration uses application-owned ports instead of infrastructure");
    }

    static ArchRule portInterfaces(String feature) {
        return classes()
                .that()
                .resideInAnyPackage(feature + ".application.port.in..", feature + ".application.port.out..")
                .should()
                .beInterfaces()
                .because("the accepted port packages contain capability contracts");
    }

    static ArchRule portDependencies(String feature) {
        return noClasses()
                .that()
                .resideInAnyPackage(feature + ".application.port.in..", feature + ".application.port.out..")
                .should()
                .dependOnClassesThat(DescribedPredicate.<JavaClass>describe(
                                "managed JPA entities", type -> type.isAnnotatedWith(Entity.class))
                        .or(JavaClass.Predicates.resideInAnyPackage(
                                feature + ".api..",
                                feature + ".persistence..",
                                feature + ".storage..",
                                "org.springframework..",
                                "jakarta.servlet..",
                                "jakarta.persistence..",
                                "org.hibernate..",
                                "java.sql..",
                                "javax.sql..",
                                "java.io..",
                                "java.nio.file..",
                                "javax.imageio..")))
                .because("port contracts expose neither managed entities nor transport/provider infrastructure");
    }

    static ArchRule api(String feature) {
        return noClasses()
                .that()
                .resideInAPackage(feature + ".api..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        feature + ".domain..",
                        feature + ".persistence..",
                        feature + ".storage..",
                        "org.springframework.data..",
                        "org.springframework.jdbc..")
                .because("inbound HTTP adapters delegate to application capabilities");
    }

    static ArchRule outboundAdaptersDoNotAccessUseCase(String feature, Class<?> useCase) {
        return noClasses()
                .that()
                .resideInAnyPackage(feature + ".persistence..", feature + ".storage..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(useCase.getName())
                .because("outbound adapters implement output contracts instead of calling the migrated use case");
    }
}
