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
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getFUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.OK

@Slf4j
@Integration
class DigitalObjectIdentifiersProfileFunctionalSpec extends BaseFunctionalSpec {

    @Shared
    Folder folder

    @Shared
    UUID complexDataModelId

    @Shared
    UUID simpleDataModelId

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

    @Override
    String getResourcePath() {
        ''
    }

    String getProfilePath() {
        'uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.profile/DigitalObjectIdentifiersProfileProviderService'
    }

    void 'test getting profile providers'() {
        when:
        HttpResponse<List<Map>> localResponse = GET('profiles/providers', Argument.listOf(Map))

        then:
        verifyResponse HttpStatus.OK, localResponse
        localResponse.body().find { it.name == "DigitalObjectIdentifiersProfileProviderService" }
    }

    void 'test get all models in profile'() {
        when:
        GET("profiles/${getProfilePath()}/DataModel")

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().size() == 2
        responseBody().count == 0 //currently empty
    }

    void 'test save profile for model'() {
        given:
        String id = getSimpleDataModelId()

        when:
        POST("dataModels/${id}/profile/${getProfilePath()}",
             [description: 'test desc', publisher: 'FT'])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sections.size() == 3
        responseBody().label == "Simple Test DataModel"
    }

    void 'test getting unused profiles on datamodel'() {
        given:
        String id = getComplexDataModelId()

        when:
        HttpResponse<List<Map>> localResponse = GET("dataModels/${id}/profiles/unused", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size() == 2
        localResponse.body().find { it.name == 'DigitalObjectIdentifiersProfileProviderService' }
        localResponse.body().find { it.displayName == 'Digital Object Identifiers DataCite Dataset Schema' }
    }
}
