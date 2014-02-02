package org.javers.core.metamodel.property

import com.google.gson.reflect.TypeToken
import org.javers.core.model.DummyAddress
import org.javers.core.model.DummyUser
import org.javers.core.model.DummyUserDetails
import org.joda.time.LocalDateTime
import spock.lang.Shared
import spock.lang.Specification

import static org.javers.core.metamodel.property.PropertiesAssert.assertThat

/**
 * @author pawel szymczyk
 */
abstract class PropertyScannerTest extends Specification {

    @Shared PropertyScanner propertyScanner

    def shouldScanPrivateProperty() {
        when:
        def properties = propertyScanner.scan(ManagedClass)

        then:
        assertThat(properties).hasProperty("privateProperty")
    }

    def shouldScanId() {
        when:
        def properties = propertyScanner.scan(DummyUser)

        then:
        assertThat(properties).hasProperty("name").looksLikeId()
    }

    def shouldScanValueMapProperty() {
        when:
        def properties = propertyScanner.scan(DummyUser)

        then:
        assertThat(properties).hasProperty("valueMap")
                              .hasJavaType(new TypeToken<Map<String, LocalDateTime>>(){}.getType())
    }

    def shouldScanEntityReferenceProperty() {
        when:
        def properties = propertyScanner.scan(DummyUser)

        then:
        assertThat(properties).hasProperty("supervisor")
                              .hasJavaType(DummyUser)
    }

    def shouldScanInheritedProperty() {
        when:
        def properties = propertyScanner.scan(DummyUser)

        then:
        assertThat(properties).hasProperty("inheritedInt")
                              .hasJavaType(int)
    }

    def shouldNotScanTransientProperty() {
        when:
        def properties = propertyScanner.scan(DummyUser)

        then:
        assertThat(properties).hasntGotProperty("someTransientField")
    }


    def shouldScanSetProperty() {
        when:
        def properties = propertyScanner.scan(DummyUser)

        then:
        assertThat(properties).hasProperty("stringSet")
                              .hasJavaType(new TypeToken<Set<String>>(){}.getType())
    }

    def shouldScanListProperty() {
        when:
        def properties = propertyScanner.scan(DummyUser)

        then:
        assertThat(properties).hasProperty("integerList")
                              .hasJavaType(new TypeToken<List<Integer>>(){}.getType())
    }

    def shouldScanArrayProperty() {
        when:
        def properties = propertyScanner.scan(DummyUser)

        then:
        assertThat(properties).hasProperty("intArray")
                              .hasJavaType(int[])
    }

    def shouldScanIntProperty() {
        when:
        def properties = propertyScanner.scan(DummyUser)

        then:
        assertThat(properties).hasProperty("age")
                              .hasJavaType(Integer.TYPE)
    }

    def shouldScanEnumProperty() {
        when:
        def properties = propertyScanner.scan(DummyUser)

        then:
        assertThat(properties).hasProperty("sex")
                              .hasJavaType(DummyUser.Sex)
    }

    def shouldScanIntegerProperty() {
        when:
        def properties = propertyScanner.scan(DummyUser)

        then:
        assertThat(properties).hasProperty("largeInt")
                              .hasJavaType(Integer)
    }

    def shouldScanBooleanProperty() {
        when:
        def properties = propertyScanner.scan(DummyUser)

        then:
        assertThat(properties).hasProperty("flag")
                              .hasJavaType(Boolean.TYPE)
    }

    def shouldScanBigBooleanProperty() {
        when:
        def properties = propertyScanner.scan(DummyUser)

        then:
        assertThat(properties).hasProperty("bigFlag")
                              .hasJavaType(Boolean)
    }

    def shouldScanStringProperty() {
        when:
        def properties = propertyScanner.scan(DummyUser)

        then:
        assertThat(properties).hasProperty("name")
                              .hasJavaType(String)
    }

    def shouldScanValueObjectProperty() {
        when:
        def properties = propertyScanner.scan(DummyUserDetails)

        then:
        assertThat(properties).hasProperty("dummyAddress")
                              .hasJavaType(DummyAddress)
    }

    class ManagedClass {
        int privateProperty
        int getPrivateProperty() {
            privateProperty
        }
    }
}