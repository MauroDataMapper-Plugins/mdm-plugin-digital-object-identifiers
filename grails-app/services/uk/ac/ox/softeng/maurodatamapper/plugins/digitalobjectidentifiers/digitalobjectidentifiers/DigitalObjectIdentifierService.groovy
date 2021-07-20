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
package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.digitalobjectidentifiers

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.DoiStatusEnum
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.profile.DigitalObjectIdentifiersProfileProviderService

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
@Transactional
class DigitalObjectIdentifierService {

    @Autowired(required = false)
    List<MultiFacetAwareService> multiFacetAwareServices


    MetadataService metadataService
    DigitalObjectIdentifiersProfileProviderService digitalObjectIdentifiersProfileProviderService

    static final String INTERNAL_DOI_NAMESPACE = 'internal'
    static final String IDENTIFIER_KEY = 'identifier'
    static final String STATUS_KEY = 'status'

    MultiFacetAware getMultiFacetAwareItemByDoi(String doi) {
        Metadata md = getIdentifierMetadataByDoi(doi)
        findMultiFacetAwareService(md.multiFacetAwareItemDomainType).get(md.multiFacetAwareItemId)

    }

    void updateDoiStatus(String doi, DoiStatusEnum status) {
        Metadata identifier = getIdentifierMetadataByDoi(doi)
        Metadata statusMetadata =
            metadataService.findAllByMultiFacetAwareItemIdAndNamespace(identifier.multiFacetAwareItemId, buildNamespaceInternal())
                .find { it.key == STATUS_KEY }
        statusMetadata.value = status
        metadataService.save(statusMetadata)
    }

    Metadata getIdentifierMetadataByDoi(String doi) {
        Metadata.byNamespaceAndKey(buildNamespaceInternal(), IDENTIFIER_KEY).eq('value', doi).get()
    }

    void retireDoi(String doi) {
        updateDoiStatus(doi, DoiStatusEnum.RETIRED)
    }

    String getDoiStatus(String doi) {
        Metadata identifierMetadata = getIdentifierMetadataByDoi(doi)
        metadataService.findAllByMultiFacetAwareItemIdAndNamespace(identifierMetadata.multiFacetAwareItemId, buildNamespaceInternal())
            .find { it.key == STATUS_KEY }.value
    }


    List<Metadata> getByMultiFacetAwareDomainTypeAndId(String domainType, String multiFacetAwareId) {
        List<Metadata> metadataList = new ArrayList<>()
        List<Metadata> temp = metadataService.findAllByMultiFacetAwareItemIdAndNamespace(UUID.fromString(multiFacetAwareId), buildNamespaceInternal())
        metadataList.add(temp.find { it.key == IDENTIFIER_KEY })
        metadataList.add(temp.find { it.key == STATUS_KEY })
        metadataList
    }


    String buildNamespaceInternal() {
        "${digitalObjectIdentifiersProfileProviderService.metadataNamespace}.${INTERNAL_DOI_NAMESPACE}"
    }


    MultiFacetAwareService findMultiFacetAwareService(String multiFacetAwareDomainType) {
        MultiFacetAwareService service = multiFacetAwareServices.find { it.handles(multiFacetAwareDomainType) }
        if (!service) throw new ApiBadRequestException('DOIS01', "No supporting service for ${multiFacetAwareDomainType}")
        return service
    }

}