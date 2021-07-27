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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.profile.DigitalObjectIdentifiersProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.web.client.DigitalObjectIdentifiersServerClient

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

@Slf4j
@Transactional
class DigitalObjectIdentifiersService {

    MetadataService metadataService
    DigitalObjectIdentifiersProfileProviderService digitalObjectIdentifiersProfileProviderService

    @Autowired
    List<MultiFacetAwareService> multiFacetAwareServices

    ApplicationContext applicationContext

    static final String INTERNAL_DOI_NAMESPACE = 'internal'
    static final String IDENTIFIER_KEY = 'identifier'
    static final String STATUS_KEY = 'status'

    MultiFacetAware findMultiFacetAwareItemByDoi(String doi) {
        Metadata md = findIdentifierMetadataByDoi(doi)
        if (!md) return null
        findMultiFacetAwareService(md.multiFacetAwareItemDomainType).get(md.multiFacetAwareItemId)
    }

    void updateDoiStatus(String doi, DoiStatusEnum status) {
        Metadata identifier = findIdentifierMetadataByDoi(doi)
        Metadata statusMetadata =
            metadataService.findAllByMultiFacetAwareItemIdAndNamespace(identifier.multiFacetAwareItemId, buildNamespaceInternal())
                .find {it.key == STATUS_KEY}
        statusMetadata.value = status
        metadataService.save(statusMetadata)
    }

    Metadata findIdentifierMetadataByDoi(String doi) {
        Metadata.byNamespaceAndKey(buildNamespaceInternal(), IDENTIFIER_KEY).eq('value', doi).get()
    }

    void retireDoi(String doi) {
        updateDoiStatus(doi, DoiStatusEnum.RETIRED)
    }

    String getDoiStatus(String doi) {
        Metadata identifierMetadata = findIdentifierMetadataByDoi(doi)
        metadataService.findAllByMultiFacetAwareItemIdAndNamespace(identifierMetadata.multiFacetAwareItemId, buildNamespaceInternal())
            .find {it.key == STATUS_KEY}.value
    }


    Map<String, String> findDoiInformationByMultiFacetAwareItemId(String domainType, UUID multiFacetAwareItemId) {
        List<Metadata> metadataList = metadataService.findAllByMultiFacetAwareItemIdAndNamespace(multiFacetAwareItemId, buildNamespaceInternal())
        if (!metadataList) return [:]
        [identifier: metadataList.find {it.key == IDENTIFIER_KEY}.value,
         status    : metadataList.find {it.key == STATUS_KEY}.value]
    }


    String buildNamespaceInternal() {
        "${digitalObjectIdentifiersProfileProviderService.metadataNamespace}.${INTERNAL_DOI_NAMESPACE}"
    }


    MultiFacetAwareService findMultiFacetAwareService(String multiFacetAwareDomainType) {
        metadataService.findServiceForMultiFacetAwareDomainType(multiFacetAwareDomainType)
    }

    def retireDoi(MultiFacetAware multiFacetAware, String submissionType) {

        String status = getStatus(multiFacetAware)
        if (status == 'retired') throw new ApiBadRequestException('sd', 'already retired')

        //TODO
        //read metadata prefix suffix
        //xml body generation
        //create retirement body
            //if draft, event:register
            //if findable, event:hide

        //update the internal NS MD to retired
        multiFacetAware.status = 'retired'

        //save multiFacetAware

        return multiFacetAware
    }

    def submitDoi(MultiFacetAware multiFacetAware, String submissionType) {

        String status = getStatus(multiFacetAware)

        if (status == 'finalised') throw new ApiBadRequestException('sd', 'already submitted')
        if (status == 'retired') throw new ApiBadRequestException('sd', 'already retired')

        List<ApiProperty> apiPropertyList = ApiProperty.findAllByCategory(BootStrap.DOI_API_PROPERTY_CATEGORY)

        ApiProperty endpointProperty = apiPropertyList.find {it.key == 'endpoint'}
        ApiProperty prefixProperty = apiPropertyList.find {it.key == 'prefix'}
        ApiProperty usernameProperty = apiPropertyList.find {it.key == 'username'}
        ApiProperty passwordProperty = apiPropertyList.find {it.key == 'password'}
        ApiProperty siteUrlProperty = apiPropertyList.find {it.key == 'site.url'}

        Map body = createDraftDoiBody(multiFacetAware, prefixProperty)

        DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient(endpointProperty.value,
                                                                                                                             "dois/${prefixProperty.value}",
                                                                                                                             applicationContext)
        if(!body.url){
            String url = "${siteUrlProperty.value}/#/doi/${prefixProperty.value}/${}"
        }

        Map<String,Object> responseBody = digitalObjectIdentifiersServerClient.sendMapToClient('',
                                                                                               body,
                                                                                               usernameProperty,
                                                                                               passwordProperty)

        //TODO:
        //how is the data found? is a get call required here?
        //update doi md with the new identifier
        //possibly update the metadata's fields which are automatically generated by doi
        //update internal status
        //save profile data

    }

    Map createAttributesBlock(MultiFacetAware multiFacetAware) {

        //TODO delete method? replace with xml generation?

        def profile = digitalObjectIdentifiersProfileProviderService.createProfileFromEntity(multiFacetAware)

        List<Map<String, Object>> sectionsData = profile.sections.collect {section ->
            Map<String, Object> data = section.fields.collectEntries {field ->
                [field.fieldName, field.currentValue]
            }
            data.findAll {k, v -> v}
        }

        Map body = [:]
        sectionsData.each {sd ->
            body.putAll(sd)
        }

        body
    }

    Map createDraftDoiBody(MultiFacetAware multiFacetAware, String prefix) {

        //todo xml body - createAttributes method to become createXml method?

        Map doiBody = '''
{
    "data": {
        "type": "dois",
        "attributes": {
            "prefix": "''' + prefix + '",'
        + createAttributesBlock(multiFacetAware) + '''
        }
    }
}
''' as Map

        doiBody
    }

    Map createPublishDoiBody(MultiFacetAware multiFacetAware, String prefix, String suffix) {

        //TODO xmlBody

        Map doiBody = '''
{
    "data": {
        "id": "''' + prefix + '/' + suffix + '''",
        "type": "dois",
        "attributes": {
            "event":"publish",
            "doi": "''' + prefix + '/' + suffix + '''",
            "prefix": "''' + prefix + '''",
            "suffix": "''' + suffix + '''",
            "xml":"''' + xmlBody + '''"

        }
    }
}
'''
        doiBody
    }

    String getStatus(MultiFacetAware multiFacetAware) {
        List<Metadata> internalMetadata = metadataService.findAllByMultiFacetAwareItemIdAndNamespace(multiFacetAware.id,
                                                                                                     "${digitalObjectIdentifiersProfileProviderService.metadataNamespace}.internal")
        internalMetadata.find {it.key = 'status'}
    }

    MultiFacetAware findMultiFacetAwareByDomainTypeAndId(String domainType, String multiFacetAwareItemIdString) {
        findMultiFacetAwareItemByDomainTypeAndId(domainType, UUID.fromString(multiFacetAwareItemIdString))
    }

    MultiFacetAware findMultiFacetAwareItemByDomainTypeAndId(String domainType, UUID multiFacetAwareItemId) {
        MultiFacetAwareService service = multiFacetAwareServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('DOIS02', "Facet retrieval for catalogue item [${domainType}] with no supporting service")
        service.get(multiFacetAwareItemId)
    }
}