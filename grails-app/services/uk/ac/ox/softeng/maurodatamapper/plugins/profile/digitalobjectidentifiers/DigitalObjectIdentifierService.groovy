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

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.Admin.DoiStatusEnum

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

@Slf4j
@Transactional
class DigitalObjectIdentifierService {

    @Autowired
    static MessageSource messageSource

    CatalogueItemService catalogueItemService
    MetadataService metadataService

    static namespaceDoi = 'org.datacite.internal'
    //todo get namespace from properties, swipe code from properties task after merge
    static doiAddressKey = 'doiAddress'
    static doiStatusKey = 'doiStatus'

    List<CatalogueItem> getCatalogueItemsByDoi(String doiLink, String namespace = namespaceDoi) {

        return catalogueItemService.findAllByMetadataNamespaceAndKey(namespaceDoiBuilder(doiLink, namespace), doiAddressKey)
        // if this doesn't work will have get metadata then walk backwards

    }

    void saveDoiForCatalogueItem(String creator, CatalogueItem catalogueItem, String doiLink,
                                 String namespace = namespaceDoi) {
        //I want to save a created DOI in the metaData of a catalogue item
        def mdList = new ArrayList<Metadata>()
        mdList.add(new Metadata(creator: creator, namespace: namespaceDoiBuilder(doiLink, namespace), key: doiAddressKey, value: doiLink))
        mdList.add(new Metadata(creator: creator, namespace: namespaceDoiBuilder(doiLink, namespace), key: doiStatusKey, value: DoiStatusEnum.ACTIVE))

        mdList.each { metadataService.addFacetToDomain(it, catalogueItem.getDomainType(), catalogueItem.getId()) }

        checkAndSave(messageSource, catalogueItem)
    }

    void updateDoiStatus(String doiLink, DoiStatusEnum status) {
        //finding the catalogue item, getting the metadata, changing the status
        List<CatalogueItem> catalogueItemList = getCatalogueItemsByDoi(doiLink)
        catalogueItemList.each {
            it.metadata.find { it.key == doiStatusKey }.value = status
            checkAndSave(messageSource, it)
        }

    }

    void retireDoi(String doiLink) {
        updateDoiStatus(doiLink, DoiStatusEnum.RETIRED)
    }

    String namespaceDoiBuilder(String doiLink, String namespace) {
        return namespace + '/' + doiLink
    }

    String getDoiStatus(String doiLink) {
        List<CatalogueItem> catalogueItemList = getCatalogueItemsByDoi(doiLink)

        catalogueItemList.find()
            .metadata.find { it.key == doiStatusKey }.value
        }

    }