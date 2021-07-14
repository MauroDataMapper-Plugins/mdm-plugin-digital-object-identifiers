package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileService
import uk.ac.ox.softeng.maurodatamapper.profile.provider.ProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getFUNCTIONAL_TEST

@Slf4j
@Integration
class DigitalObjectIdentifiersServiceSpec extends BaseFunctionalSpec {

    @Shared
    Folder folder

    @Shared
    UUID complexDataModelId

    @Shared
    UUID simpleDataModelId

    ProfileService profileService

    @Override
    String getResourcePath() {
        ''
    }

    String getProfilePath() {
        'uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers/DigitalObjectIdentifiersService'
    }

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        folder.addToMetadata(new Metadata(namespace: "test.namespace", key: "propertyKey", value: "propertyValue", createdBy: FUNCTIONAL_TEST))
        checkAndSave(folder)
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: FUNCTIONAL_TEST)
        checkAndSave(testAuthority)

        complexDataModelId = BootstrapModels.buildAndSaveComplexDataModel(messageSource, folder, testAuthority).id
        simpleDataModelId = BootstrapModels.buildAndSaveSimpleDataModel(messageSource, folder, testAuthority).id

        sessionFactory.currentSession.flush()
    }



    void 'test'() {
        given:
        String id = getComplexDataModelId()
        ProfileProviderService profileProviderService =
            profileService.findProfileProviderService('uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.profile',
                                                      'DigitalObjectIdentifiersProfileProviderService',
                                                      (getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'))

        when:
        POST("dataModels/${id}/profile/${getProfilePath()}",
             [description: 'test desc', publisher: 'FT'])

        then:
        profileProviderService
        verifyResponse HttpStatus.OK, response
        //profileService.findMultiFacetAwareItemById(responseBody().id)
        responseBody().sections.size() == 3
        responseBody().label == "Simple Test DataModel"
    }

}
