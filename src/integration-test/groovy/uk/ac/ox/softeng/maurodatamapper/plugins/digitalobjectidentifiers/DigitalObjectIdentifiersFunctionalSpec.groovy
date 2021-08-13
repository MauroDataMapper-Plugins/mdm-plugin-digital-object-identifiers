/*
 * Copyright 2020-2021 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
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

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

@Slf4j
@Integration
class DigitalObjectIdentifiersFunctionalSpec extends BaseFunctionalSpec {

    DigitalObjectIdentifiersService digitalObjectIdentifiersService
    DigitalObjectIdentifiersProfileProviderService digitalObjectIdentifiersProfileProviderService

    @Shared
    String doiString

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

        doiString = '10.4124/kzn3hb2vh8.1'
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataModelFunctionalSpec')
        ApiProperty.findByKey('site.url').delete(flush: true)
        cleanUpResources(Folder, Classifier, SemanticLink)
    }

    @Override
    String getResourcePath() {
        ''
    }

    //    GET /api/doi/${digitalObjectIdentifier}
    void 'DC01 test retrieving a MultiFacetItem via DOI'() {
        given:
        String id = getStatusCheckId()

        when:
        GET("doi/$doiString")

        then:
        verifyResponse(HttpStatus.OK, response)
        assert responseBody().id == id

        cleanup:
        DELETE("dataModels/$id?permanent=true")
        verifyResponse(HttpStatus.NO_CONTENT, response)
    }

    //    GET /api/{multiFacetAwareDomainType}/{multiFacetAwareId}/doi
    void 'DC02 test retrieving a DOI via MultiFacet Item Domain and ID'() {
        given:
        String id = getStatusCheckId()

        when:
        GET("dataModels/$id/doi")

        then:
        verifyResponse(HttpStatus.OK, response)
        assert responseBody().status == 'draft'
        assert responseBody().identifier == doiString

        cleanup:
        DELETE("dataModels/$id?permanent=true")
        verifyResponse(HttpStatus.NO_CONTENT, response)

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
        GET("dataModels/$id/profile/" +
            "${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(HttpStatus.OK, response)

        responseBody().id
        responseBody().sections.get(0).fields.find { it.metadataPropertyName == 'creators/creator/creatorName' }.currentValue ==
        'Creator Anthony Char'
        responseBody().sections.get(0).fields.find { it.metadataPropertyName == 'titles/title' }.currentValue == 'DOI DataCite BDI title'
        responseBody().sections.get(0).fields.find { it.metadataPropertyName == 'publisher' }.currentValue == 'Publisher Anthony'
        responseBody().sections.get(0).fields.find { it.metadataPropertyName == 'publicationYear' }.currentValue == '2021'
        responseBody().sections.get(0).fields.find { it.metadataPropertyName == 'suffix' }.currentValue

        cleanup:
        cleanUpDataModel(id)
    }

    void 'test finalise from new Doi endpoint end to end'() {
        given:
        String id = buildTestDataModel()

        when:
        POST("dataModels/${id}/doi?submissionType=finalise", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/" +
            "${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

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
        verifyResponse(HttpStatus.OK, response)

        when:
        POST("dataModels/${id}/doi?submissionType=finalise", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/" +
            "${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

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
        verifyResponse(HttpStatus.OK, response)
        POST("dataModels/${id}/doi?submissionType=finalise", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        POST("dataModels/${id}/doi?submissionType=retire", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/" +
            "${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

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
        verifyResponse(HttpStatus.OK, response)

        when:
        POST("dataModels/${id}/doi?submissionType=retire", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/" +
            "${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

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
        verifyResponse(HttpStatus.OK, response)
        POST("dataModels/${id}/doi?submissionType=finalise", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        POST("dataModels/${id}/doi?submissionType=retire", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/" +
            "${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id

        cleanup:
        cleanUpDataModel(id)
    }

    void 'test draft from new Doi endpoint end to end with full profile metadata'() {
        given:
        String id = buildFullTestDataModel()

        when:
        POST("dataModels/${id}/doi?submissionType=draft", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/" +
            "${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(HttpStatus.OK, response)

        responseBody().id
        responseBody().sections.get(0).fields.find { it.metadataPropertyName == 'creators/creator/creatorName' }.currentValue ==
        'Full Creator Anthony'
        responseBody().sections.get(0).fields.find { it.metadataPropertyName == 'titles/title' }.currentValue == 'Full DOI DataCite BDI title'
        responseBody().sections.get(0).fields.find { it.metadataPropertyName == 'publisher' }.currentValue == 'Full Publisher Anthony'
        responseBody().sections.get(0).fields.find { it.metadataPropertyName == 'publicationYear' }.currentValue == '2021'
        responseBody().sections.get(0).fields.find { it.metadataPropertyName == 'suffix' }.currentValue

        cleanup:
        cleanUpDataModel(id)
    }

    String buildTestDataModel() {
        POST("folders/$folderId/dataModels", [
            label: 'Functional Test Model'
        ]
        )
        verifyResponse(HttpStatus.CREATED, response)
        String id = responseBody().id

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'identifiers/identifier',
            value    : 'Test Identifier'
        ])
        verifyResponse(HttpStatus.CREATED, response)


        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'creators/creator/creatorName',
            value    : 'Creator Anthony Char'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'titles/title',
            value    : 'DOI DataCite BDI title'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'publisher',
            value    : 'Publisher Anthony'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'publicationYear',
            value    : '2021'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'resourceType',
            value    : 'Dataset'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        id
    }

    String buildFullTestDataModel() {
        POST("folders/$folderId/dataModels", [
            label: 'Functional Test Model'
        ]
        )
        verifyResponse(HttpStatus.CREATED, response)
        String id = responseBody().id

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'identifiers/identifier',
            value    : 'Test Identifier'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'creators/creator/creatorName',
            value    : 'Full Creator Anthony'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'creators/creator/affiliation',
            value    : 'Full Creator Affiliation'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        //did not populate
        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'creators/creator/nameIdentifier',
            value    : 'Full Creator Name ID'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'titles/title',
            value    : 'Full DOI DataCite BDI title'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'publisher',
            value    : 'Full Publisher Anthony'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'publicationYear',
            value    : '2021'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'resourceType',
            value    : 'Dataset'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'subjects/subject',
            value    : ['Full Subject 1', 'Full Subject 2']
        ])
        verifyResponse(HttpStatus.CREATED, response)

        //Contributor Type "not supported in schema 4."
//        //did not populate
//        POST("dataModels/$id/metadata", [
//            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
//            key      : 'contributors/contributor/contributorName',
//            value    : 'Full Contributor Anthony'
//        ])
//        verifyResponse(HttpStatus.CREATED, response)
//
//        //did not populate
//        POST("dataModels/$id/metadata", [
//            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
//            key      : 'contributors/contributor/nameIdentifier',
//            value    : 'Full Contributor Name ID'
//        ])
//        verifyResponse(HttpStatus.CREATED, response)
//
//        //did not populate
//        POST("dataModels/$id/metadata", [
//            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
//            key      : 'contributors/contributor/affiliation',
//            value    : 'Full Contributor Affiliation'
//        ])
//        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'dates/date',
            value    : '17/04/2021'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'language',
            value    : 'English'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        //did not populate
        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'alternateIdentifiers/alternateIdentifier',
            value    : 'Full Alternate Identifier'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        //did not populate
        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'relatedIdentifiers/relatedIdentifier',
            value    : 'Full Related Identifier'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'sizes/size',
            value    : 'Full Size'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'formats/format',
            value    : 'Full Format'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'version',
            value    : 'Full Version'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'rightsList/rights',
            value    : 'Full Rights'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'description',
            value    : 'Full Description Field 1'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'fundingReferences/fundingReference/funderName',
            value    : 'Full Funder Name Anthony'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        //did not populate
        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'fundingReferences/fundingReference/fundingIdentifier',
            value    : 'Full Funding Identifier'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'fundingReferences/fundingReference/awardNumber',
            value    : 'Full Award Number'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
            key      : 'fundingReferences/fundingReference/awardTitle',
            value    : 'Full Award Title'
        ])
        verifyResponse(HttpStatus.CREATED, response)

        id
    }

    String getStatusCheckId() {
        POST("folders/$folderId/dataModels", [
            label: 'Functional Test DataModel'
        ])
        verifyResponse(HttpStatus.CREATED, response)
        String id = responseBody().id

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersService.buildNamespaceInternal(),
            key: DigitalObjectIdentifiersService.IDENTIFIER_KEY,
            value: doiString
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersService.buildNamespaceInternal(),
            key      : digitalObjectIdentifiersService.STATUS_KEY,
            value    : DoiStatusEnum.DRAFT.toString()
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
