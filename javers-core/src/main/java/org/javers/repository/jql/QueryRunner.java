package org.javers.repository.jql;

import org.javers.common.exception.JaversException;
import org.javers.common.exception.JaversExceptionCode;
import org.javers.core.ObjectChange;
import org.javers.core.diff.Change;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.shadow.Shadow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author bartosz.walacik
 */
public class QueryRunner {
    private static final Logger logger = LoggerFactory.getLogger(JqlQuery.JQL_LOGGER_NAME);

    private final ChangesQueryRunner changesQueryRunner;
    private final SnapshotQueryRunner snapshotQueryRunner;
    private final ShadowStreamQueryRunner shadowStreamQueryRunner;

    QueryRunner(ChangesQueryRunner changesQueryRunner, SnapshotQueryRunner snapshotQueryRunner, ShadowStreamQueryRunner shadowStreamQueryRunner) {
        this.changesQueryRunner = changesQueryRunner;
        this.snapshotQueryRunner = snapshotQueryRunner;
        this.shadowStreamQueryRunner = shadowStreamQueryRunner;
    }

    public Stream<Shadow> queryForShadowsStream(JqlQuery query) {
        return shadowStreamQueryRunner.queryForShadowsStream(query);
    }

    public Optional<CdoSnapshot> runQueryForLatestSnapshot(GlobalIdDTO globalId) {
        return snapshotQueryRunner.runQueryForLatestSnapshot(globalId);
    }

    public List<CdoSnapshot> queryForSnapshots(JqlQuery query) {
        validateSnapshotQueryLimit(query, "findSnapshots()");
        return snapshotQueryRunner.queryForSnapshots(query);
    }

    public List<Change> queryForChanges(JqlQuery query) {
        validateSnapshotQueryLimit(query, "findChanges()");
        return changesQueryRunner.queryForChanges(query);
    }

    public List<ObjectChange> queryForChangesFromDB(JqlQuery query) {
        validateSnapshotQueryLimit(query, "findChangesFromDB()");
        return changesQueryRunner.queryForChangesFromDB(query);
    }

    private void validateSnapshotQueryLimit(JqlQuery query, String method) {
        if (query.getQueryParams().hasSnapshotQueryLimit()) {
            throw new JaversException(JaversExceptionCode.MALFORMED_JQL,
                    "QueryBuilder.snapshotQueryLimit() can be used only in Shadow queries. " +
                    "You can't use it with "+method+
                    ". Please do not confuse QueryBuilder.snapshotQueryLimit() with QueryBuilder.limit().");
        }
    }
}
