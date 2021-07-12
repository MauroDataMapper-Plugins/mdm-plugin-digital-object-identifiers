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
package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.service

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.DoiStatusEnum
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.digitalobjectidentifiers.DigitalObjectIdentifierService
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Slf4j
@Integration
@Rollback
class DigitalObjectIdentifierIntegrationSpec extends BaseIntegrationSpec {


    DataModel simpleDataModel
    DataModel complexDataModel
    String doiString
    String namespaceString

    DigitalObjectIdentifierService digitalObjectIdentifierService

    @Override
    void setupDomainData() {
        log.debug('Setting up DigitalObjectIdentifierService')
        complexDataModel = buildComplexDataModel()
        simpleDataModel = buildSimpleDataModel()

        doiString = 'testDoiDs01'
        namespaceString = digitalObjectIdentifierService.INTERNAL_DOI_NAMESPACE

        simpleDataModel.addToMetadata(new Metadata(
            namespace: digitalObjectIdentifierService.buildNamespaceInternal(),
            key: digitalObjectIdentifierService.IDENTIFIER_KEY,
            value: doiString, createdBy: StandardEmailAddress.INTEGRATION_TEST))
        simpleDataModel.addToMetadata(new Metadata(
            namespace: digitalObjectIdentifierService.buildNamespaceInternal(),
            key: digitalObjectIdentifierService.STATUS_KEY,
            value: DoiStatusEnum.ACTIVE, createdBy: StandardEmailAddress.INTEGRATION_TEST))

        checkAndSave(simpleDataModel)
    }

    @Override
    void preDomainDataSetup() {
        checkAndSave(new Folder(label: 'catalogue', createdBy: StandardEmailAddress.INTEGRATION_TEST))
        checkAndSave(new Authority(label: 'Test Authority', url: 'http:localhost', createdBy: StandardEmailAddress.INTEGRATION_TEST))
    }


    void 'DS01 Testing getting an existing MultiFacetAware using the DOI'() {
        given: "A Stored dataModel with doiMetadata"
        setupData()

        when: "using the DOI to get the DataModel"
        MultiFacetAware multiFacetAware = digitalObjectIdentifierService.getMultiFacetAwareItemByDoi(doiString)

        then: "the dataModel containing the doi MetaData should be returned"
        multiFacetAware.id == simpleDataModel.id
    }


    void 'DS02 Updating the status of a DOI link'() {
        given: "A Stored dataModel with doiMetadata"
        setupData()

        when: 'the call is made to change the status'
        digitalObjectIdentifierService.updateDoiStatus(doiString, DoiStatusEnum.TEST)
        then: 'the model should have an updated status'
        digitalObjectIdentifierService.getDoiStatus(doiString) == DoiStatusEnum.TEST.toString()

        when: 'the call is made to change the status to retired'
        digitalObjectIdentifierService.retireDoi(doiString)
        then: 'the model should have an updated status of "retired"'
        digitalObjectIdentifierService.getDoiStatus(doiString) == DoiStatusEnum.RETIRED.toString()
    }

    DataModel buildSimpleDataModel() {
        BootstrapModels.buildAndSaveSimpleDataModel(messageSource, testFolder, testAuthority)
    }

    DataModel buildComplexDataModel() {
        BootstrapModels.buildAndSaveComplexDataModel(messageSource, testFolder, testAuthority)
    }

    Folder getTestFolder() {
        Folder.findByLabel('catalogue')
    }

    Authority getTestAuthority() {
        Authority.findByLabel('Test Authority')
    }
}

