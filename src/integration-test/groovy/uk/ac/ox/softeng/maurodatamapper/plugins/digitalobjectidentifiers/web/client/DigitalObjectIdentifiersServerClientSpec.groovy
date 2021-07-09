package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.web.client

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

@Slf4j
@Integration
class DigitalObjectIdentifiersServerClientSpec extends BaseIntegrationSpec {

    DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient

    @Autowired
    ApplicationContext applicationContext

    @Override
    void setupDomainData() {
    }

    void 'DOIP-01: Test the DOI profile count endpoint'() {
        given:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient('https://api.test.datacite.org/',
                                                                                        '',
                                                                                        applicationContext)

        when:
        def result = digitalObjectIdentifiersServerClient.getDoiProfileCount()

        then:
        result instanceof Map
        result.size() == 3
        result.links.self == 'https://api.test.datacite.org/dois?_summary=count&_format=json'
        result.data.size == 25
    }

    void 'DOIP-02: Test the DOI profile summary endpoint'() {
        given:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient('https://api.test.datacite.org/',
                                                                                        '',
                                                                                        applicationContext)

        when:
        def result = digitalObjectIdentifiersServerClient.getDoiProfiles(2)

        then:
        result instanceof Map
        result.size() == 3
        result.links.self == 'https://api.test.datacite.org/dois?_summary=text&_count=2&_format=json'
        result.data.size == 25
    }

    void 'DOIP-03: Test the DOI profile entry endpoint'() {
        given:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient('https://api.test.datacite.org/',
                                                                                        '',
                                                                                        applicationContext)
        String entryId = '10.21985/n2-f3hw-vf87'

        when:
        def result = digitalObjectIdentifiersServerClient.getDoiProfileEntry(entryId)

        then:
        result instanceof Map
        result.size() == 1
        result.data.type == 'dois'
        result.data.id == entryId

    }

    void 'PUB-01: Test the DOI profile entry endpoint'() {
        given:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient('https://api.test.datacite.org/',
                                                                                        applicationContext)
        String entryId = '10.21985/n2-f3hw-vf87'

        when:
        def result = digitalObjectIdentifiersServerClient.getDoiProfileEntry(entryId)

        then:
        result instanceof Map
        result.size() == 1
        result.data.type == 'dois'
        result.data.id == entryId

    }

    void 'GEN-01: Test the DOI profile entry id for non-existent entry'() {
        given:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient('https://api.test.datacite.org/',
                                                                                        '',
                                                                                        applicationContext)
        String entryId = 'non-existent-entry'

        when:
        digitalObjectIdentifiersServerClient.getDoiProfileEntry(entryId)

        then:
        ApiBadRequestException ex = thrown(ApiBadRequestException)
        ex.message == 'Requested endpoint could not be found https://api.test.datacite.org/dois/non-existent-entry?_format=json'
    }

}
