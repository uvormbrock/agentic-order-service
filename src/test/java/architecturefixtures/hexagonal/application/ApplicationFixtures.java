package architecturefixtures.hexagonal.application;

import architecturefixtures.hexagonal.application.port.in.InputPort;
import architecturefixtures.hexagonal.application.port.out.OutputFixtures.OutputPort;
import architecturefixtures.hexagonal.persistence.PersistenceFixtures;
import java.nio.file.Path;

public class ApplicationFixtures {
    public static class UseCase implements InputPort {
        private final OutputPort output;

        public UseCase(OutputPort output) {
            this.output = output;
        }

        public String execute() {
            return output.load();
        }
    }

    public static class InvalidRepositoryAccess {
        PersistenceFixtures.Adapter adapter;
    }

    public static class InvalidFilesystemAccess {
        Path path;
    }

    public static class InvalidIntegrationAccess {
        architecturefixtures.hexagonal.integration.IntegrationFixtures adapter;
    }

    public static class InvalidTransactionAdapterAccess {
        architecturefixtures.hexagonal.transaction.TransactionFixtures adapter;
    }
}
