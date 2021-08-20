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
package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.profile


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import org.junit.Assert

import static io.micronaut.http.HttpStatus.OK

@Slf4j
@Integration
class DigitalObjectIdentifiersProfileFunctionalSpec extends BaseFunctionalSpec {

    DigitalObjectIdentifiersProfileProviderService digitalObjectIdentifiersProfileProviderService

    @Transactional
    UUID getFolderId() {
        Folder.findByLabel('Functional Test Folder').id
    }


    @Override
    String getResourcePath() {
        ''
    }

    String getProfilePath() {
        "$digitalObjectIdentifiersProfileProviderService.namespace/${digitalObjectIdentifiersProfileProviderService.name}"
    }

    void 'test getting profile providers'() {
        when:
        HttpResponse<List<Map>> localResponse = GET('profiles/providers', Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().find {it.name == "DigitalObjectIdentifiersProfileProviderService"}
    }

    void 'test get all models in profile'() {
        when:
        GET("profiles/${getProfilePath()}/DataModel")

        then:
        verifyResponse OK, response
        responseBody().size() == 2
        responseBody().count == 0 //currently empty
    }

    void 'test getting unused profiles on datamodel'() {
        given:
        String id = buildTestDataModel()

        when:
        HttpResponse<List<Map>> localResponse = GET("dataModels/${id}/profiles/unused", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size() == 2
        localResponse.body().find {it.name == 'DigitalObjectIdentifiersProfileProviderService'}
        localResponse.body().find {it.displayName == 'Digital Object Identifiers DataCite Dataset Schema'}

        cleanup:
        cleanUpDataModel(id)
    }

    void 'test save minimum profile for model'() {
        given:
        String id = buildTestDataModel()
        Map profileMap = [
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

        ]

        when:
        POST("profiles/${getProfilePath()}/dataModels/${id}/validate", profileMap)

        then:
        verifyResponse(OK, response)

        when:
        POST("profiles/${getProfilePath()}/dataModels/${id}", profileMap)

        then:
        verifyResponse OK, response
        verifyProfile()

        cleanup:
        cleanUpDataModel(id)
    }

    void 'test save full profile for model'() {
        given:
        String id = buildTestDataModel('An interesting description')
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

        when:
        POST("profiles/${getProfilePath()}/dataModels/${id}/validate", profileMap)

        then:
        verifyResponse(OK, response)

        when:
        POST("profiles/${getProfilePath()}/dataModels/${id}", profileMap)

        then:
        verifyResponse OK, response
        verifyProfile('An interesting description',true)

        cleanup:
        cleanUpDataModel(id)
    }

    String buildTestDataModel(String description = null) {
        POST("folders/$folderId/dataModels", [
            label      : 'Functional Test Model',
            description: description
        ]
        )
        verifyResponse(HttpStatus.CREATED, response)
        String id = responseBody().id
        PUT("dataModels/$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        id
    }

    void verifyProfile(String expectedDescription = '', boolean full = false) {
        verifyFieldData 'Predefined/Supplied Fields', 'prefix', '10.80079'
        verifyFieldData 'Predefined/Supplied Fields', 'suffix', ''
        verifyFieldData 'Predefined/Supplied Fields', 'identifier', ''
        verifyFieldData 'Predefined/Supplied Fields', 'status', 'not submitted'
        verifyFieldData 'Predefined/Supplied Fields', 'titles/mainTitle', 'Functional Test Model'
        verifyFieldData 'Predefined/Supplied Fields', 'descriptions/mainDescription', expectedDescription
        verifyFieldData 'Predefined/Supplied Fields', 'version', '1.0.0'

        verifyFieldData 'Primary Creator', 'creators/creator/creatorName', 'Creator Anthony Char'
        verifyFieldData 'Additional Mandatory Fields', 'publisher', 'Publisher Anthony'
        verifyFieldData 'Additional Mandatory Fields', 'publicationYear', '2021'

        if (full) {
            verifyFieldData 'Primary Creator', 'creators/creator/creatorNameType', 'Personal'
            verifyFieldData 'Primary Creator', 'creators/creator/givenName', 'a given name'
            verifyFieldData 'Primary Creator', 'creators/creator/familyName', 'a family name'
            verifyFieldData 'Primary Creator', 'creators/creator/nameIdentifier', 'a name identifier'
            verifyFieldData 'Primary Creator', 'creators/creator/affiliation', 'testing'

            verifyFieldData 'Additional Optional Title Section', 'titles/title', 'Full DOI DataCite BDI title'
            verifyFieldData 'Additional Optional Title Section', 'titles/titleType', 'AlternativeTitle'

            verifyFieldData 'Additional Optional Description Section', 'descriptions/description', 'Lots of very important text'
            verifyFieldData 'Additional Optional Description Section', 'descriptions/descriptionType', 'TechnicalInfo'

            verifyFieldData 'Primary Contributor', 'contributors/contributor/contributorName', 'Creator Anthony Char'
            verifyFieldData 'Primary Contributor', 'contributors/contributor/contributorNameType', 'Personal'
            verifyFieldData 'Primary Contributor', 'contributors/contributor/contributorType', 'DataCollector'
            verifyFieldData 'Primary Contributor', 'contributors/contributor/givenName', 'a given name'
            verifyFieldData 'Primary Contributor', 'contributors/contributor/familyName', 'a family name'
            verifyFieldData 'Primary Contributor', 'contributors/contributor/nameIdentifier', 'a name identifier'
            verifyFieldData 'Primary Contributor', 'contributors/contributor/affiliation', 'testing'

            verifyFieldData 'Additional Optional Fields', 'language', 'en'
        }
    }

    void verifyFieldData(String section, String field, String expectedValue, boolean valueKnown = true) {
        Map fieldData = responseBody().sections.find {it.name == section}.fields.find {it.metadataPropertyName == field}
        Assert.assertNotNull("[${section}].[$field] should exist", fieldData)
        if (!valueKnown) Assert.assertNotNull "Value", fieldData.currentValue
        else Assert.assertEquals "[${section}].[$field] should be", expectedValue, fieldData.currentValue
    }


    void cleanUpDataModel(String id) {
        DELETE("dataModels/$id?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
        sleep(20)
    }
}
