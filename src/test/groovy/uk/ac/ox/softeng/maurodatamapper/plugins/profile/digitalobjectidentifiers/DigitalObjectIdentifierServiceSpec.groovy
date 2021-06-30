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
package uk.ac.ox.softeng.maurodatamapper.plugins.profile.digitalobjectidentifiers

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTreeService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.Admin.DoiStatusEnum
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.gorm.transactions.Transactional
import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getINTEGRATION_TEST

@Slf4j
@Transactional
class DigitalObjectIdentifierServiceSpec extends BaseUnitSpec implements ServiceUnitTest<DigitalObjectIdentifierService> {


    DataModel simpleDataModel
    DataModel complexDataModel
    String doiString
    String doiNamespaceString


    def setup() {

        mockArtefact(BreadcrumbTreeService)
        mockArtefact(DataTypeService)
        mockArtefact(DataClassService)
        mockArtefact(DataElementService)

        mockDomains(DataModel, DataClass, Metadata, DataType, DataElement, PrimitiveType, Folder, Authority)

        checkAndSave(new Folder(label: 'catalogue', createdBy: StandardEmailAddress.UNIT_TEST))
        checkAndSave(new Authority(label: 'Test Authority', url: 'http:localhost', createdBy: StandardEmailAddress.UNIT_TEST))
        checkAndSave(new Authority(label: 'Mauro Data Mapper', url: 'http://localhost', createdBy: StandardEmailAddress.UNIT_TEST))


        service.catalogueItemService = Mock(DataModelService) {
            List<CatalogueItem> catalogueItemList = new ArrayList<>()
            findAllByMetadataNamespaceAndKey(_, _) >> { String namespace, String key ->
                if (namespace == doiNamespaceString)
                    catalogueItemList.add(simpleDataModel)
                return catalogueItemList
            }
        }
        service.metadataService = Mock(MetadataService) {
            addFacetToDomain(_, _, _) >> { Metadata md, String domain, UUID bid ->
                if (simpleDataModel.id == bid) {
                    simpleDataModel.addToMetadata(md)
                    md.multiFacetAwareItem = simpleDataModel
                } else if (complexDataModel.id == bid) {
                    complexDataModel.addToMetadata(md)
                    md.multiFacetAwareItem = complexDataModel
                }
            }

        }
    }

    def cleanup() {
    }


    void 'DS01 Testing adding doi metadata to a dataModel'() {

        given: "a simple data model and DOI"
        simpleDataModel = buildSimpleDataModel()
        doiString = 'testDoiDs01'
        doiNamespaceString = service.namespaceDoiBuilder(doiString, service.namespaceDoi)

        when: "saving the DOI meta data as part of the dataModel"
        service.saveDoiForCatalogueItem(INTEGRATION_TEST, simpleDataModel, doiString)

        then: "the dataModel should have two metadata entries, one of which is the DOI and the other of which is the status"
        simpleDataModel.metadata.find { it.namespace == doiNamespaceString }
        simpleDataModel.metadata.find { (it.key == service.doiAddressKey) && (it.value == doiString) }
        simpleDataModel.metadata.find { (it.key == service.doiStatusKey) && (it.value == DoiStatusEnum.ACTIVE.toString()) }

    }

    void 'DS02 Testing getting an existing datamodel using the DOI'() {
        // this test seems a bit like giving myself a medal because the stubs dictate the outcome
        // the other tests assume this part works
        given: "Stored a datamodel with doiMetadata"
        simpleDataModel = buildSimpleDataModel()
        complexDataModel = buildComplexDataModel()

        doiString = 'testDoiDs02'
        doiNamespaceString = service.namespaceDoiBuilder(doiString, service.namespaceDoi)

        service.saveDoiForCatalogueItem(INTEGRATION_TEST, simpleDataModel, doiString)

        when: "using the DOI to get the DataModel"
        List<CatalogueItem> catalogueItemList = service.getCatalogueItemsByDoi(doiString)

        then: "the dataModel containing the doi MetaData should be returned, the one without should not"
        catalogueItemList.find { it.id == simpleDataModel.id }
        !catalogueItemList.find { it.id == complexDataModel.id }
    }

    void 'DS03 Updating the status of a DOI link'() {

        given: 'A dataModel with doi metadata, and a status'
        simpleDataModel = buildSimpleDataModel()
        doiString = 'testDoiDs03'
        doiNamespaceString = service.namespaceDoiBuilder(doiString, service.namespaceDoi)
        service.saveDoiForCatalogueItem(INTEGRATION_TEST, simpleDataModel, doiString)

        when: 'the call is made to change the status'
        service.updateDoiStatus(doiString, DoiStatusEnum.TEST)
        then: 'the model should have an updated status'
        service.getDoiStatus(doiString) == DoiStatusEnum.TEST.toString()

        when: 'the call is made to change the status to retired'
        service.retireDoi(doiString)
        then: 'the model should have an updated status of "retired"'
        service.getDoiStatus(doiString) == DoiStatusEnum.RETIRED.toString()
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
