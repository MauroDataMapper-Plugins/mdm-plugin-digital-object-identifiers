package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.web.client

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.core.GrailsApplication
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

@Slf4j
@Integration
class DigitalObjectIdentifiersServerClientSpec extends BaseIntegrationSpec {

    DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient
    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    ApplicationContext applicationContext

    @Override
    void setupDomainData() {
    }

    void 'DOI-01: Test posting to DataCite'() {
        given:
        Map props = grailsApplication.config.maurodatamapper.digitalobjectidentifiers
        String entryId = UUID.randomUUID()
        log.debug(entryId)
        String body = '''
{
    "data":
    {
        "attributes":
        {
            "doi":"10.80079/''' + entryId + '''"
        }
    }
}
'''

        expect:
        props
        props.username
        props.password
        props.prefix == '10.80079'
        props.endpoint == 'https://api.test.datacite.org'

        when:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient(props.endpoint,
                                                                                        "dois",
                                                                                        applicationContext)

        and:
        digitalObjectIdentifiersServerClient.sendMapToClient('',
                                                             body,
                                                             props.username,
                                                             props.password)

        then:
        digitalObjectIdentifiersServerClient.retrieveMapFromClient(props.prefix + '/' + entryId,
                                                                   props.username,
                                                                   props.password)

    }

    void 'DOI-02: Test posting to DataCite without usr/pwd authentication'() {
        given:
        Map props =  grailsApplication.config.maurodatamapper.digitalobjectidentifiers
        String entryId = UUID.randomUUID()
        log.debug(entryId)
        String body = '''
{
    "data":
    {
        "type":"dois",
        "attributes":
        {
            "doi":"10.80079/''' + entryId + '''"
        }
    }
}
'''

        expect:
        props
        props.username
        props.password
        props.prefix == '10.80079'
        props.endpoint == 'https://api.test.datacite.org'

        when:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient(props.endpoint,
                                                                                        "dois",
                                                                                        applicationContext)

        and:
        digitalObjectIdentifiersServerClient.sendMapToClient('',
                                                             body,
                                                             '',
                                                             '')

        then:
        ApiBadRequestException ex = thrown(ApiBadRequestException)
        ex.status.code == 400
    }

    void 'DOI-03: Test posting to DataCite with unauthorised DOI prefix'() {
        given:
        Map props =  grailsApplication.config.maurodatamapper.digitalobjectidentifiers
        String entryId = UUID.randomUUID()
        log.debug(entryId)
        String body = '''
{
    "data":
    {
        "type":"dois",
        "attributes":
        {
            "doi":"10.00000/''' + entryId + '''"
        }
    }
}
'''

        expect:
        props
        props.username
        props.password
        props.prefix == '10.80079'
        props.endpoint == 'https://api.test.datacite.org'

        when:
        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient(props.endpoint,
                                                                                        "dois",
                                                                                        applicationContext)

        and:
        digitalObjectIdentifiersServerClient.sendMapToClient('',
                                                             body,
                                                             props.username,
                                                             props.password)

        then:
        ApiInternalException ex = thrown(ApiInternalException)
        ex.cause.response.status.code == 403
    }

}
