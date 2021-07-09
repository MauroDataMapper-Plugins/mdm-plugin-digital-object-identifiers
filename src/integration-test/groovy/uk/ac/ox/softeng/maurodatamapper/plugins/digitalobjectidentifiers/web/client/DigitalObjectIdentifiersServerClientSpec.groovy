package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.web.client

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

class DigitalObjectIdentifiersServerClientSpec extends BaseIntegrationSpec {

    DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient

    @Autowired
    ApplicationContext applicationContext

    @Override
    void setupDomainData() {
    }

    //TODO check versionPath
    //TODO change 'then' counts to match results
    //TODO PUB-03

    void 'DOIP-01: Test the DOI profile count endpoint'() {
        given:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient('https://api.test.datacite.org/',
                                                                                        '2.0',
                                                                                        applicationContext)

        when:
        def result = digitalObjectIdentifiersServerClient.getDoiProfileCount()

        then:
        result instanceof Map
        result.total == 0
        result.resourceType == 'Bundle'
        result.id
    }

    void 'DOIP-02: Test the DOI profile summary endpoint'() {
        given:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient('https://api.test.datacite.org/',
                                                                                        '2.0',
                                                                                        applicationContext)

        when:
        def result = digitalObjectIdentifiersServerClient.getDoiProfiles(2)

        then:
        result instanceof Map
        result.total == 0
        result.entry instanceof List
        result.entry.size() == 2
        result.entry.first().resource.id
    }

    void 'DOIP-03: Test the DOI profile entry endpoint'() {
        given:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient('https://api.test.datacite.org/',
                                                                                        '2.0',
                                                                                        applicationContext)
        String entryId = 'CareConnect-Condition-1'

        when:
        def result = digitalObjectIdentifiersServerClient.getDoiProfileEntry(entryId)

        then:
        result instanceof Map
        result.id == entryId
        result.resourceType == 'StructureDefinition'
        result.name == entryId

    }

    void 'PUB-01: Test the DOI profile count endpoint'() {
        given:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient('https://api.test.datacite.org/',
                                                                                        applicationContext)

        when:
        def result = digitalObjectIdentifiersServerClient.getDoiProfileCount()

        then:
        result instanceof Map
        result.total == 0
        result.resourceType == 'Bundle'
        result.id
    }

    void 'PUB-02: Test the DOI profile summary endpoint'() {
        given:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient('https://api.test.datacite.org/',
                                                                                        applicationContext)

        when:
        def result = digitalObjectIdentifiersServerClient.getDoiProfiles(2)

        then:
        result instanceof Map
        result.total == 0
        result.entry instanceof List
        result.entry.size() == 2
        result.entry.first().resource.id
    }

    void 'PUB-03: Test the DOI profile entry endpoint'() {
        given:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient('https://api.test.datacite.org/',
                                                                                        applicationContext)
        String entryId = 'CareConnect-Condition-1'

        when:
        def result = digitalObjectIdentifiersServerClient.getDoiProfileEntry(entryId)

        then:
        result instanceof Map
        result.id == entryId
        result.resourceType == 'dois'
        result.name == entryId

    }

    void 'VER-01: Test the DOI profile count endpoint with a blank version'() {
        given:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient('https://api.test.datacite.org/',
                                                                                        '',
                                                                                        applicationContext)

        when:
        def result = digitalObjectIdentifiersServerClient.getDoiProfileCount()

        then:
        result instanceof Map
        result.total == 0
        result.resourceType == 'Bundle'
        result.id
    }

    void 'GEN-01: Test the DOI profile entry id for non-existent entry'() {
        given:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient('https://api.test.datacite.org/',
                                                                                        '2.0',
                                                                                        applicationContext)
        String entryId = 'non-existent-entry'

        when:
        digitalObjectIdentifiersServerClient.getDoiProfileEntry(entryId)

        then:
        ApiBadRequestException ex = thrown(ApiBadRequestException)
        ex.message == 'Requested endpoint could not be found https://api.test.datacite.org/dois/non-existent-entry?_format=json'
    }

}
