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
package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.controller

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.DoiStatusEnum
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.digitalobjectidentifiers.DigitalObjectIdentifierService
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus

@Slf4j
@Integration
class DigitalObjectIdentifierControllerFunctionalSpec extends BaseFunctionalSpec {

    DigitalObjectIdentifierService digitalObjectIdentifierService
    DataModel simpleDataModel
    String doiString

    @Override
    String getResourcePath() {
        'doi'
    }


    @Transactional
    def checkAndSetupData() {
        log.debug('Setting up DigitalObjectIdentifierController Data')
        simpleDataModel = buildSimpleDataModel()
        doiString = 'TestDoiAddressString'
        simpleDataModel.addToMetadata(new Metadata(
            namespace: digitalObjectIdentifierService.buildNamespaceInternal(),
            key: digitalObjectIdentifierService.IDENTIFIER_KEY,
            value: doiString, createdBy: StandardEmailAddress.FUNCTIONAL_TEST))
        simpleDataModel.addToMetadata(new Metadata(
            namespace: digitalObjectIdentifierService.buildNamespaceInternal(),
            key: digitalObjectIdentifierService.STATUS_KEY,
            value: DoiStatusEnum.DRAFT.key, createdBy: StandardEmailAddress.FUNCTIONAL_TEST))
        checkAndSave(simpleDataModel)
    }

    @OnceBefore
    @Transactional
    void preDomainDataSetup() {
        checkAndSave(new Folder(label: 'catalogue', createdBy: StandardEmailAddress.INTEGRATION_TEST))
        checkAndSave(new Authority(label: 'Test Authority', url: 'http:localhost', createdBy: StandardEmailAddress.INTEGRATION_TEST))
    }


    //    GET /api/doi/${digitalObjectIdentifier}
    void 'DC01 test retrieving a MultiFacetItem via DOI'() {
        given:
        checkAndSetupData()
        when:
        GET("$doiString")

        then:
        verifyResponse(HttpStatus.OK, response)
        assert responseBody().label == simpleDataModel.label

    }

    //    GET /api/{multiFacetAwareDomainType}/{multiFacetAwareId}/doi
    void 'DC02 test retrieving a DOI via MultiFacet Item Domain and ID'() {
        given:
        checkAndSetupData()
        when:
        GET("$simpleDataModel.domainType/$simpleDataModel.id/doi", MAP_ARG, true)

        then:
        verifyResponse(HttpStatus.OK, response)
        assert responseBody().find { it.key == 'status' }.value == responseJson().find { it.key == 'status' }.value
        assert responseBody().find { it.key == 'identifier' }.value == responseJson().find { it.key == 'identifier' }.value

    }

    DataModel buildSimpleDataModel() {
        BootstrapModels.buildAndSaveSimpleDataModel(messageSource, testFolder, testAuthority)
    }

    Folder getTestFolder() {
        Folder.findByLabel('catalogue')
    }

    Authority getTestAuthority() {
        Authority.findByLabel('Test Authority')
    }

    Map responseJson() {
        [identifier: 'TestDoiAddressString', status: 'draft']
    }
}
