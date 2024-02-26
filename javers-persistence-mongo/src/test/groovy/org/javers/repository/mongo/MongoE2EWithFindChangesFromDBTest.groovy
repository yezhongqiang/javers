package org.javers.repository.mongo

import org.javers.core.model.DummyUser
import org.javers.repository.api.QueryParamsBuilder

import static org.javers.core.model.DummyUser.dummyUser

class MongoE2EWithFindChangesFromDBTest extends MongoE2ETest {

    def "should find correct changes with id and text"() {
        given:
        MongoRepository mongoRepository = (MongoRepository)repository

        def kazikV1 = dummyUser("kazik").withAge(12)
                .withIntArray(List.of(1,2,3,4, 12))
                .withIntegerList(List.of(2,3))
                .withSurname("kafka")
        def kazikV2 = dummyUser("kazik").withAge(13);

        javers.commit("andy", kazikV1)
        kazikV1.withSurname("5.0").withAge(15)
                .withIntArray(List.of(2,100))
                .withIntegerList(List.of(3,4));
        javers.commit("andy", kazikV1)
        kazikV2.withSupervisor(kazikV1)
        javers.commit("andy", kazikV2)

        def id = javersTestBuilder.instanceId(new DummyUser("kazik"))
        def queryParams = QueryParamsBuilder
                .withLimit(10)
                .withSearchText("5.0")
                .withRegex(true)
                .build()

        when:
        def diffs = mongoRepository.getChangeHistoryFromDB(id, queryParams)

        then:
        diffs.size() == 1
        def diff = diffs.get(0)
        diff.getCommitMetadata().getAuthor() == "andy"
        diff.getSnapshotVersion() == 2
    }

    def "should find correct changes with only text"() {
        given:
        MongoRepository mongoRepository = (MongoRepository)repository

        def kazikV1 = dummyUser("kazik").withAge(12).withSurname("blabla")
        def kazikV2 = dummyUser("test").withAge(13)

        javers.commit("andy", kazikV1)
        kazikV1.withSurname("kafka").withAge(15)
        javers.commit("andy", kazikV1)
        javers.commit("andy", kazikV2)

        def queryParams = QueryParamsBuilder.withLimit(2).withSearchText("15").build()

        when:
        def diffs = mongoRepository.getChangesFromDB(queryParams)

        then:
        diffs.size() == 1
        def diff = diffs.get(0)
        diff.getCommitMetadata().getAuthor() == "andy"
        diff.getSnapshotVersion() == 2
    }
}
