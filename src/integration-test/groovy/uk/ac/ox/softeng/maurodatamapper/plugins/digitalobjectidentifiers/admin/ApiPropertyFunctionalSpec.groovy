package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.admin

import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.BootStrap
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus

/**
 * @since 30/06/2021
 */
@Slf4j
@Integration
class ApiPropertyFunctionalSpec extends BaseFunctionalSpec{
    @Override
    String getResourcePath() {
        'admin/properties'
    }

    void 'test getting api properties to check doi properties are loaded'(){
        when:
        GET('')

        then:
        verifyResponse(HttpStatus.OK, response)

        when:
        List<Map> props = responseBody().items.findAll {it.category == BootStrap.DOI_API_PROPERTY_CATEGORY}

        then:
        props
        props.size() == 4
        props.find{it.key == 'prefix' && it.value == '10.80079'}
        props.find{it.key == 'endpoint' && it.value == 'https://api.test.datacite.org'}
        props.find{it.key == 'username' && it.value == 'NOT_SET'}
        props.find{it.key == 'password' && it.value == 'NOT_SET'}
    }
}
