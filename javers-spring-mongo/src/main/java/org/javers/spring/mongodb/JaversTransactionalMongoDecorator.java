package org.javers.spring.mongodb;

import java.util.List;
import java.util.Map;
import org.javers.core.Javers;
import org.javers.core.commit.Commit;
import org.javers.repository.jql.GlobalIdDTO;
import org.javers.spring.transactions.JaversTransactionalDecorator;

/**
 * Transactional wrapper for core JaVers instance.
 * Provides integration with Spring JPA TransactionManager
 *
 * @author bartosz walacik
 */
public class JaversTransactionalMongoDecorator extends JaversTransactionalDecorator {

    public JaversTransactionalMongoDecorator(Javers delegate) {
        super(delegate);
    }
}
