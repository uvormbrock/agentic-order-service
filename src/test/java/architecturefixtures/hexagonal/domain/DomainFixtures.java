package architecturefixtures.hexagonal.domain;

import architecturefixtures.hexagonal.application.ApplicationFixtures;
import jakarta.persistence.Entity;

public class DomainFixtures {
    @Entity
    public static class Aggregate {}

    public static class InvalidJpaRuntime {
        jakarta.persistence.EntityManager entityManager;
    }

    public static class InvalidDomain {
        ApplicationFixtures.UseCase useCase;
    }
}
