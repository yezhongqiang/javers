package org.javers.repository.mongo

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.MongoClientSettings
import org.bson.codecs.configuration.CodecRegistry
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import org.bson.codecs.pojo.PojoCodecProvider;

import static org.javers.core.model.DummyUser.dummyUser
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * @author bartosz walacik
 */
@Testcontainers
class MongoE2ETest extends JaversMongoRepositoryE2ETest {

  @Shared
  static DockerizedMongoContainer dockerizedMongoContainer = new DockerizedMongoContainer()

  @Shared
  static MongoClient mongoClient = dockerizedMongoContainer.mongoClient

  @Override
  protected MongoDatabase getMongoDb() {
    CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));
    MongoDatabase database = mongoClient.getDatabase("test")
    database.withCodecRegistry(pojoCodecRegistry);
    database
  }

  def "should persist head id"() {
    given:
    MongoRepository mongoRepository = (MongoRepository)repository

    def commitFactory = javersTestBuilder.commitFactory

    def kazikV1 = dummyUser("Kazik").withAge(1)
    def kazikV2 = dummyUser("Kazik").withAge(2)

    def commit1 = commitFactory.create("author", [:], kazikV1)
    def commit2 = commitFactory.create("author", [:], kazikV2)

    when:
    mongoRepository.persist(commit1)

    then:
    mongoRepository.getHeadId().getMajorId() == 1
    mongoRepository.getHeadId().getMinorId() == 0

    when:
    mongoRepository.persist(commit2)

    then:
    mongoRepository.getHeadId().getMajorId() == 1
    mongoRepository.getHeadId().getMinorId() == 1
  }
}
