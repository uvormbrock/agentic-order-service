package architecturefixtures.hexagonal.api;

import architecturefixtures.hexagonal.application.port.in.InputPort;
import architecturefixtures.hexagonal.domain.DomainFixtures;
import architecturefixtures.hexagonal.persistence.PersistenceFixtures;

public class ApiFixtures {
    public static class Controller {
        InputPort input;
    }

    public static class InvalidRepositoryAccess {
        PersistenceFixtures.Adapter adapter;
    }

    public static class InvalidEntityAccess {
        DomainFixtures.Aggregate aggregate;
    }
}
