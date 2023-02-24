/*
 * Copyright 2020-2023 University of Oxford and NHS England
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


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.profile.DigitalObjectIdentifiersProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.junit.Assert
import spock.lang.Shared

import static io.micronaut.http.HttpStatus.OK

@Slf4j
@Integration
class DigitalObjectIdentifiersFunctionalSpec extends BaseFunctionalSpec {

    DigitalObjectIdentifiersService digitalObjectIdentifiersService
    DigitalObjectIdentifiersProfileProviderService digitalObjectIdentifiersProfileProviderService

    @Shared
    String doiString = '10.4124/kzn3hb2vh8.1'

    @Transactional
    UUID getFolderId() {
        Folder.findByLabel('Functional Test Folder').id
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
        verifyResponse(OK, response)
        responseBody().id == id

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
        verifyResponse(OK, response)
        responseBody().status == 'draft'
        responseBody().identifier == doiString

        cleanup:
        DELETE("dataModels/$id?permanent=true")
        verifyResponse(HttpStatus.NO_CONTENT, response)

    }


    void 'S01 : test draft from new Doi endpoint end to end'() {
        given:
        String id = buildTestDataModel()

        when:
        POST("dataModels/${id}/doi?submissionType=draft", [:])

        then:
        verifyResponse(OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/" +
            "${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(OK, response)
        responseBody().id == id
        String identifier = verifyProfileAfterSubmission('draft', 'draft')

        when:
        GET("dataModels/$id/doi")

        then:
        verifyResponse(OK, response)
        responseBody().status == 'draft'
        responseBody().identifier == identifier

        cleanup:
        cleanUpDataModel(id)
    }

    void 'S02 : test finalise from new Doi endpoint end to end'() {
        given:
        String id = buildTestDataModel()

        when:
        POST("dataModels/${id}/doi?submissionType=finalise", [:])

        then:
        verifyResponse(OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/" +
            "${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(OK, response)
        responseBody().id == id
        String identifier =   verifyProfileAfterSubmission('final', 'findable')

        when:
        GET("dataModels/$id/doi")

        then:
        verifyResponse(OK, response)
        responseBody().status == 'final'
        responseBody().identifier == identifier

        cleanup:
        cleanUpDataModel(id)
    }

    void 'S03 : test finalise from draft Doi endpoint end to end'() {
        given:
        String id = buildTestDataModel()
        POST("dataModels/${id}/doi?submissionType=draft", [:])
        verifyResponse(OK, response)

        when:
        POST("dataModels/${id}/doi?submissionType=finalise", [:])

        then:
        verifyResponse(OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/" +
            "${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(OK, response)
        responseBody().id == id
        String identifier =    verifyProfileAfterSubmission('final', 'findable')

        when:
        GET("dataModels/$id/doi")

        then:
        verifyResponse(OK, response)
        responseBody().status == 'final'
        responseBody().identifier == identifier

        cleanup:
        cleanUpDataModel(id)

    }

    void 'S04 : test retire from finalised Doi endpoint end to end'() {
        given:
        String id = buildTestDataModel()
        POST("dataModels/${id}/doi?submissionType=draft", [:])
        verifyResponse(OK, response)
        POST("dataModels/${id}/doi?submissionType=finalise", [:])
        verifyResponse(OK, response)

        when:
        POST("dataModels/${id}/doi?submissionType=retire", [:])

        then:
        verifyResponse(OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/" +
            "${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(OK, response)
        responseBody().id == id
        String identifier =   verifyProfileAfterSubmission('retired', 'registered')

        when:
        GET("dataModels/$id/doi")

        then:
        verifyResponse(OK, response)
        responseBody().status == 'retired'
        responseBody().identifier == identifier

        cleanup:
        cleanUpDataModel(id)

    }

    void 'S05 : test retire from draft Doi endpoint end to end'() {
        given:
        String id = buildTestDataModel()
        POST("dataModels/${id}/doi?submissionType=draft", [:])
        verifyResponse(OK, response)

        when:
        POST("dataModels/${id}/doi?submissionType=retire", [:])

        then:
        verifyResponse(OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/" +
            "${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(OK, response)
        responseBody().id == id
        String identifier =    verifyProfileAfterSubmission('retired', 'registered')

        when:
        GET("dataModels/$id/doi")

        then:
        verifyResponse(OK, response)
        responseBody().status == 'retired'
        responseBody().identifier == identifier

        cleanup:
        cleanUpDataModel(id)
    }

    void 'S07 : test draft from new Doi endpoint end to end with full profile metadata'() {
        given:
        String id = buildFullTestDataModel()

        when:
        POST("dataModels/${id}/doi?submissionType=draft", [:])

        then:
        verifyResponse(OK, response)
        responseBody().id == id

        when:
        GET("dataModels/$id/profile/" +
            "${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}")

        then:
        verifyResponse(OK, response)

        responseBody().id == id
        String identifier =   verifyProfileAfterSubmission('draft', 'draft', 'An interesting description')

        when:
        GET("dataModels/$id/doi")

        then:
        verifyResponse(OK, response)
        responseBody().status == 'draft'
        responseBody().identifier == identifier

        cleanup:
        cleanUpDataModel(id)
    }

    String verifyProfileAfterSubmission(String expectedStatus, String expectedState, String expectedDescription = '') {
        verifyFieldData 'Predefined/Supplied Fields', 'prefix', '10.80079'
        verifyFieldData 'Predefined/Supplied Fields', 'suffix', null, false
        verifyFieldData 'Predefined/Supplied Fields', 'status', expectedStatus
        verifyFieldData 'Predefined/Supplied Fields', 'state', expectedState
        verifyFieldData 'Predefined/Supplied Fields', 'titles/mainTitle', 'Functional Test Model'
        verifyFieldData 'Predefined/Supplied Fields', 'descriptions/mainDescription', expectedDescription
        verifyFieldData 'Predefined/Supplied Fields', 'version', '1.0.0'

        verifyFieldData 'Primary Creator', 'creators/creator/creatorName', 'Creator Anthony Char'
        verifyFieldData 'Additional Mandatory Fields', 'publisher', 'Publisher Anthony'
        verifyFieldData 'Additional Mandatory Fields', 'publicationYear', '2021'

        String suffix = responseBody().sections.find {it.name == 'Predefined/Supplied Fields'}.fields.find {it.metadataPropertyName == 'suffix'}.currentValue
        verifyFieldData 'Predefined/Supplied Fields', 'identifier', "10.80079/$suffix"

        "10.80079/$suffix"
    }

    void verifyFieldData(String section, String field, String expectedValue, boolean valueKnown = true) {
        Map fieldData = responseBody().sections.find {it.name == section}.fields.find {it.metadataPropertyName == field}
        Assert.assertNotNull("[${section}].[$field] should exist", fieldData)
        if (!valueKnown) Assert.assertNotNull "Value", fieldData.currentValue
        else assert fieldData.currentValue == expectedValue
    }

    void validateAndSubmitProfile(String id, Map profileMap) {
        String profileUrl = "profiles/${digitalObjectIdentifiersProfileProviderService.namespace}/${digitalObjectIdentifiersProfileProviderService.name}/dataModels/${id}"
        POST("$profileUrl/validate", profileMap)
        verifyResponse(OK, response)
        POST(profileUrl, profileMap)
        verifyResponse(OK, response)
    }

    String buildTestDataModel() {
        POST("folders/$folderId/dataModels", [
            label: 'Functional Test Model'
        ]
        )
        verifyResponse(HttpStatus.CREATED, response)
        String id = responseBody().id
        PUT("dataModels/$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        validateAndSubmitProfile(id, [
            sections  : [
                [
                    name  : 'Primary Creator',
                    fields: [
                        [
                            metadataPropertyName: 'creators/creator/creatorName',
                            currentValue        : 'Creator Anthony Char'
                        ],
                    ],
                ],
                [
                    name  : 'Additional Mandatory Fields',
                    fields: [
                        [
                            metadataPropertyName: 'publisher',
                            currentValue        : 'Publisher Anthony'
                        ],
                        [
                            metadataPropertyName: 'publicationYear',
                            currentValue        : '2021'
                        ],
                        [
                            metadataPropertyName: 'resourceType',
                            currentValue        : 'Dataset'
                        ]
                    ],
                ]
            ],
            id        : id.toString(),
            label     : 'Functional Test Model',
            domainType: 'DataModel',
            namespace : digitalObjectIdentifiersProfileProviderService.namespace,
            name      : digitalObjectIdentifiersProfileProviderService.name

        ])

        id
    }

    String buildFullTestDataModel() {
        POST("folders/$folderId/dataModels", [
            label      : 'Functional Test Model',
            description: 'An interesting description'
        ]
        )
        verifyResponse(HttpStatus.CREATED, response)
        String id = responseBody().id

        PUT("dataModels/$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        Map profileMap = [
            sections  : [
                [
                    name  : 'Primary Creator',
                    fields: [
                        [
                            metadataPropertyName: 'creators/creator/creatorName',
                            currentValue        : 'Creator Anthony Char'
                        ],
                        [
                            metadataPropertyName: 'creators/creator/creatorNameType',
                            currentValue        : 'Personal'
                        ],
                        [
                            metadataPropertyName: 'creators/creator/givenName',
                            currentValue        : 'a given name'
                        ],
                        [
                            metadataPropertyName: 'creators/creator/familyName',
                            currentValue        : 'a family name'
                        ],
                        [
                            metadataPropertyName: 'creators/creator/nameIdentifier',
                            currentValue        : 'a name identifier'
                        ],
                        [
                            metadataPropertyName: 'creators/creator/affiliation',
                            currentValue        : 'testing'
                        ],
                    ],
                ],
                [
                    name  : 'Additional Mandatory Fields',
                    fields: [
                        [
                            metadataPropertyName: 'publisher',
                            currentValue        : 'Publisher Anthony'
                        ],
                        [
                            metadataPropertyName: 'publicationYear',
                            currentValue        : '2021'
                        ],
                        [
                            metadataPropertyName: 'resourceType',
                            currentValue        : 'Dataset'
                        ],
                    ],
                ],
                [
                    name  : 'Additional Optional Title Section',
                    fields: [
                        [
                            metadataPropertyName: 'titles/title',
                            currentValue        : 'Full DOI DataCite BDI title'
                        ],
                        [
                            metadataPropertyName: 'titles/titleType',
                            currentValue        : 'AlternativeTitle'
                        ],
                    ],
                ],
                [
                    name  : 'Additional Optional Description Section',
                    fields: [
                        [
                            metadataPropertyName: 'descriptions/description',
                            currentValue        : 'Lots of very important text'
                        ],
                        [
                            metadataPropertyName: 'descriptions/descriptionType',
                            currentValue        : 'TechnicalInfo'
                        ],
                    ],
                ],
                [
                    name  : 'Primary Contributor',
                    fields: [
                        [
                            metadataPropertyName: 'contributors/contributor/contributorName',
                            currentValue        : 'Creator Anthony Char'
                        ],
                        [
                            metadataPropertyName: 'contributors/contributor/contributorNameType',
                            currentValue        : 'Personal'
                        ],
                        [
                            metadataPropertyName: 'contributors/contributor/contributorType',
                            currentValue        : 'DataCollector'
                        ],
                        [
                            metadataPropertyName: 'contributors/contributor/givenName',
                            currentValue        : 'a given name'
                        ],
                        [
                            metadataPropertyName: 'contributors/contributor/familyName',
                            currentValue        : 'a family name'
                        ],
                        [
                            metadataPropertyName: 'contributors/contributor/nameIdentifier',
                            currentValue        : 'a name identifier'
                        ],
                        [
                            metadataPropertyName: 'contributors/contributor/affiliation',
                            currentValue        : 'testing'
                        ],
                    ],
                ],
                [
                    name  : 'Additional Optional Fields',
                    fields: [
                        [
                            metadataPropertyName: 'language',
                            currentValue        : 'en'
                        ],
                    ],
                ],
            ],
            id        : id.toString(),
            label     : 'Functional Test Model',
            domainType: 'DataModel',
            namespace : digitalObjectIdentifiersProfileProviderService.namespace,
            name      : digitalObjectIdentifiersProfileProviderService.name

        ]
        validateAndSubmitProfile(id, profileMap)
        id
    }

    String getStatusCheckId() {
        POST("folders/$folderId/dataModels", [
            label: 'Functional Test DataModel'
        ])
        verifyResponse(HttpStatus.CREATED, response)
        String id = responseBody().id

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersService.metadataNamespace,
            key      : DigitalObjectIdentifiersService.IDENTIFIER_KEY,
            value    : doiString
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata", [
            namespace: digitalObjectIdentifiersService.metadataNamespace,
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
