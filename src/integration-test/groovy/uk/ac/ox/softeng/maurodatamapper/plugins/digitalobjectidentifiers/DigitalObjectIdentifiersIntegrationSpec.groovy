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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Slf4j
@Integration
@Rollback
class DigitalObjectIdentifiersIntegrationSpec extends BaseIntegrationSpec {

    DataModel simpleDataModel
    String doiString

    AuthorityService authorityService
    DigitalObjectIdentifiersService digitalObjectIdentifiersService

    @Override
    void setupDomainData() {
        log.debug('Setting up DigitalObjectIdentifierService')
        Authority authority = authorityService.getDefaultAuthority()
        Folder folder = new Folder(label: 'catalogue', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        checkAndSave(folder)

        simpleDataModel = BootstrapModels.buildAndSaveSimpleDataModel(messageSource, folder, authority)

        doiString = '10.4124/kzn3hb2vh8.1'

        simpleDataModel.addToMetadata(new Metadata(
            namespace: digitalObjectIdentifiersService.metadataNamespace,
            key: DigitalObjectIdentifiersService.IDENTIFIER_KEY,
            value: doiString, createdBy: StandardEmailAddress.INTEGRATION_TEST))
        simpleDataModel.addToMetadata(new Metadata(
            namespace: digitalObjectIdentifiersService.metadataNamespace,
            key: DigitalObjectIdentifiersService.STATUS_KEY,
            value: DoiStatusEnum.DRAFT, createdBy: StandardEmailAddress.INTEGRATION_TEST))

        checkAndSave(simpleDataModel)
    }

    void 'DS01 Testing getting an existing MultiFacetAware using the DOI'() {
        given: "A Stored dataModel with doiMetadata"
        setupData()

        when: "using the DOI to get the DataModel"
        MultiFacetAware multiFacetAware = digitalObjectIdentifiersService.findMultiFacetAwareItemByDoi(doiString)

        then: "the dataModel containing the doi MetaData should be returned"
        multiFacetAware.id == simpleDataModel.id
    }
}

