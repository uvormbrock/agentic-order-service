package architecturefixtures.hexagonal.application.port.out;

import architecturefixtures.hexagonal.domain.DomainFixtures;
import org.springframework.data.repository.Repository;
import org.springframework.http.ResponseEntity;

public interface OutputFixtures {
    interface OutputPort {
        String load();
    }

    interface InvalidEntityPort {
        DomainFixtures.Aggregate load();
    }

    interface InvalidHttpPort {
        ResponseEntity<String> load();
    }

    interface InvalidRepositoryPort extends Repository<DomainFixtures.Aggregate, Long> {}

    class InvalidConcretePort {}
}
