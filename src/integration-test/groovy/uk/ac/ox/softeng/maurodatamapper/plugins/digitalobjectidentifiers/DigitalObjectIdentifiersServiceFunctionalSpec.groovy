package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.profile.DigitalObjectIdentifiersProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import java.net.http.HttpResponse

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

@Slf4j
@Integration
class DigitalObjectIdentifiersServiceFunctionalSpec extends BaseFunctionalSpec {

    DigitalObjectIdentifiersProfileProviderService digitalObjectIdentifiersProfileProviderService

    @Shared
    UUID folderId

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        sessionFactory.currentSession.flush()
        folderId = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST).save(flush: true).id
        assert folderId
        ApiProperty siteUrl = new ApiProperty(key: 'site.url', value: 'http://jenkins.cs.ox.ac.uk/mdm', createdBy: FUNCTIONAL_TEST)
        checkAndSave(siteUrl)
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataModelFunctionalSpec')
        cleanUpResources(Folder, Classifier, SemanticLink)
    }

    @Override
    String getResourcePath() {
        ''
    }

    String getProfilePath() {
        'uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers/DigitalObjectIdentifiersService'
    }


    void 'test draft from new Doi endpoint end to end'() {
        given:
        String id = buildTestDataModel()

        when:
        POST("dataModels/${id}/doi?submissionType=draft", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(HttpStatus.OK, response)

        responseBody().id
        responseBody().sections.get(0).fields.find {it.metadataPropertyName == 'creators/creator/creatorName'}.currentValue == 'Creator Anthony Char'
        responseBody().sections.get(0).fields.find {it.metadataPropertyName == 'titles/title'}.currentValue == 'DOI DataCite BDI title'
        responseBody().sections.get(0).fields.find {it.metadataPropertyName == 'publisher'}.currentValue == 'Publisher Anthony'
        responseBody().sections.get(0).fields.find {it.metadataPropertyName == 'publicationYear'}.currentValue == '2021'

        cleanup:
        cleanUpDataModel(id)
    }

    void 'test finalise from new Doi endpoint end to end'() {
        given:
        String id = buildTestDataModel()
        POST("dataModels/$id/metadata",[
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key: 'event',
            value: 'publish'
        ])

        when:
        POST("dataModels/${id}/doi?submissionType=finalise",[:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id


        cleanup:
        cleanUpDataModel(id)
    }

    void 'test finalise from draft Doi endpoint end to end'() {
        given:
        String id = buildTestDataModel()
        POST("dataModels/${id}/doi?submissionType=draft", [:])
        POST("dataModels/$id/metadata",[
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key: 'event',
            value: 'publish'
        ])

        when:
        POST("dataModels/${id}/doi?submissionType=finalise",[:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id


        cleanup:
        cleanUpDataModel(id)

    }

    void 'test finalise from registered Doi endpoint end to end'() {
        given:
        String id = buildTestDataModel()
        POST("dataModels/${id}/doi?submissionType=draft", [:])
        POST("dataModels/$id/metadata",[
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key: 'event',
            value: 'register'
        ])
        POST("dataModels/${id}/doi?submissionType=retire", [:])
        POST("dataModels/$id/metadata",[
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key: 'event',
            value: 'publish'
        ])

        when:
        POST("dataModels/${id}/doi?submissionType=finalise",[:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id


        cleanup:
        cleanUpDataModel(id)

    }

    void 'test retire from draft Doi endpoint end to end'() {
        given:
        String id = buildTestDataModel()
        POST("dataModels/${id}/doi?submissionType=draft", [:])

        when:
        POST("dataModels/${id}/doi?submissionType=retire",[:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id

        cleanup:
        cleanUpDataModel(id)
    }

    void 'test retire from finalise Doi endpoint end to end'() {
        given:
        String id = buildTestDataModel()
        POST("dataModels/${id}/doi?submissionType=draft", [:])
        POST("dataModels/$id/metadata",[
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key: 'event',
            value: 'publish'
        ])
        POST("dataModels/${id}/doi?submissionType=finalise",[:])

        when:
        POST("dataModels/${id}/doi?submissionType=retire",[:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id

        cleanup:
        cleanUpDataModel(id)

    }

    String buildTestDataModel(){
        POST("folders/$folderId/dataModels", [
            label: 'Functional Test Model'
        ]
        )
        verifyResponse(HttpStatus.CREATED, response)
        String id = responseBody().id

        POST("dataModels/$id/metadata",[
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key: 'identifiers/identifier',
            value: 'Test Identifier'
        ])
        verifyResponse(HttpStatus.CREATED, response)


        POST("dataModels/$id/metadata",[
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key: 'creators/creator/creatorName',
            value: 'Creator Anthony Char'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata",[
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key: 'titles/title',
            value: 'DOI DataCite BDI title'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata",[
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key: 'publisher',
            value: 'Publisher Anthony'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata",[
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key: 'publicationYear',
            value: '2021'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata",[
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key: 'resourceType',
            value: 'Dataset'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        id
    }

    void cleanUpDataModel(String id) {
        if (id) {
            DELETE("dataModels/$id?permanent=true")
            assert response.status() == HttpStatus.NO_CONTENT
            sleep(20)
        }
    }
}
