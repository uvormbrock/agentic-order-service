package architecturefixtures.hexagonal.persistence;

import architecturefixtures.hexagonal.application.ApplicationFixtures;
import architecturefixtures.hexagonal.application.port.out.OutputFixtures.OutputPort;

public class PersistenceFixtures {
    public static class Adapter implements OutputPort {
        public String load() {
            return "fixture";
        }
    }

    public static class InvalidUseCaseAccess {
        ApplicationFixtures.UseCase useCase;
    }
}
