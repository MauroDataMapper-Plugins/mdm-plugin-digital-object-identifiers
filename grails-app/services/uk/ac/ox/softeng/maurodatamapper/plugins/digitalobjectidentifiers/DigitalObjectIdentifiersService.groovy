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
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.profile.DigitalObjectIdentifiersProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.web.client.DigitalObjectIdentifiersServerClient

import grails.gorm.transactions.Transactional
import grails.plugin.markup.view.MarkupViewTemplateEngine
import groovy.text.Template
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

@Slf4j
@Transactional
class DigitalObjectIdentifiersService {

    @Autowired
    MarkupViewTemplateEngine markupViewTemplateEngine

    MetadataService metadataService
    DigitalObjectIdentifiersProfileProviderService digitalObjectIdentifiersProfileProviderService

    @Autowired
    List<MultiFacetAwareService> multiFacetAwareServices

    @Autowired
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
        if (status == 'retired') throw new ApiBadRequestException('sd01', 'already retired')

        List<ApiProperty> apiPropertyList = ApiProperty.findAllByCategory(BootStrap.DOI_API_PROPERTY_CATEGORY)

        ApiProperty endpointProperty = apiPropertyList.find {it.key == 'endpoint'}
        ApiProperty prefixProperty = apiPropertyList.find {it.key == 'prefix'}
        ApiProperty usernameProperty = apiPropertyList.find {it.key == 'username'}
        ApiProperty passwordProperty = apiPropertyList.find {it.key == 'password'}
        ApiProperty siteUrlProperty = apiPropertyList.find {it.key == 'site.url'}

        DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient(endpointProperty.value,
                                                                                                                             "dois",
                                                                                                                             applicationContext)

        String suffix = multiFacetAware.suffix

        Map<String,Object> responseBody = digitalObjectIdentifiersServerClient.retrieveMapFromClient(prefixProperty + '/' + suffix,
                                                                                                   usernameProperty,
                                                                                                   passwordProperty)

        if (responseBody.status == 'draft') {
            Map retireBody = createDoiBody(xmlEncoded, prefixProperty, suffix, 'register')
        }
        else if (responseBody.status == 'findable') {
            Map retireBody = createDoiBody(xmlEncoded, prefixProperty, suffix, 'hide')
        }
        else {
            throw new ApiBadRequestException('sd02', 'Incompatible status of DOI.')
        }

        Map<String,Object> responseFinalBody = digitalObjectIdentifiersServerClient.putMapToClient('',
                                                                                                   retireBody,
                                                                                                   usernameProperty,
                                                                                                   passwordProperty)

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

        DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient(endpointProperty.value,
                                                                                                                             "dois",
                                                                                                                             applicationContext)
        Map attributesBlock = createAttributesBlock(multiFacetAware)

        if (!attributesBlock.suffix) {
            submitAsSimple(digitalObjectIdentifiersServerClient, attributesBlock, prefixProperty, usernameProperty, passwordProperty)
        }

        if(submissionType == 'draft'){
            submitAsDraft(digitalObjectIdentifiersServerClient, attributesBlock, prefixProperty, usernameProperty, passwordProperty)
        }

        if(submissionType == 'retire'){
            submitAsRetire(digitalObjectIdentifiersServerClient, attributesBlock)
        }

        if(submissionType == 'finalise'){
            if(!attributesBlock.suffix){
                attributesBlock = submitAsDraft(digitalObjectIdentifiersServerClient, attributesBlock)
                // make sure all data saved and attributesBlock is updated (especially the suffix)
            }
            submitAsFinal(digitalObjectIdentifiersServerClient, attributesBlock)
        }


        ///////////

//        String suffix = responseDraftBody.suffix
//        multiFacetAware.suffix = responseDraftBody.suffix
//        multiFacetAware.status = responseDraftBody.status
//
//        Map finalBody = createEventDoiBody(multiFacetAware, prefixProperty, suffix, 'publish')
//        if(!finalBody.url){
//            String url = "${siteUrlProperty.value}/#/doi/${prefixProperty.value}/${suffix}"
//        }
//        Map<String,Object> responseFinalBody = digitalObjectIdentifiersServerClient.putMapToClient('',
//                                                                                               finalBody,
//                                                                                               usernameProperty,
//                                                                                               passwordProperty)


        //save profile data

    }

    def submitAsSimple(DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient, Map attributesBlock, ApiProperty prefixProperty,
                       ApiProperty usernameProperty, ApiProperty passwordProperty) {

        Map simpleBody = createDoiBody(prefixProperty.value)
        Map<String,Object> responseBody = digitalObjectIdentifiersServerClient.sendMapToClient('',
                                                                                               simpleBody,
                                                                                               usernameProperty.value,
                                                                                               passwordProperty.value)

        updateFromResponse(responseBody, attributesBlock)
    }

    def submitAsDraft(DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient, Map attributesBlock, ApiProperty prefixProperty,
                      ApiProperty siteUrlProperty, ApiProperty usernameProperty, ApiProperty passwordProperty){

        Map xmlEncoded = createDataCiteXml()
        Map draftBody = createDraftDoiBody(xmlEncoded, prefixProperty.value)
        if(!draftBody.url) {
            String url = "${siteUrlProperty.value}/#/doi/${prefixProperty.value}/${}"
        }

        Map<String,Object> responseBody = digitalObjectIdentifiersServerClient.putMapToClient(prefixProperty.value + '/' + attributesBlock.suffix,
                                                                                              draftBody,
                                                                                              usernameProperty.value,
                                                                                              passwordProperty.value)

        updateFromResponse(responseBody, attributesBlock)
    }

    def submitAsFinal(DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient, Map attributesBlock, ApiProperty prefixProperty,
                      ApiProperty suffixProperty, ApiProperty siteUrlProperty, ApiProperty usernameProperty, ApiProperty passwordProperty){

        Map finalBody = createEventDoiBody(attributesBlock, prefixProperty.value, suffixProperty.value, 'publish')
        if(!finalBody.url) {
            String url = "${siteUrlProperty.value}/#/doi/${prefixProperty.value}/${suffixProperty.value}"
        }

        Map<String,Object> responseBody = digitalObjectIdentifiersServerClient.putMapToClient('',
                                                                                               finalBody,
                                                                                               usernameProperty,
                                                                                               passwordProperty)

        updateFromResponse(responseBody, attributesBlock)
    }

    def updateFromResponse( Map<String,Object> responseBody, Map attributesBlock){

        //TODO this functionality
        attributesBlock.suffix = responseBody.data.attributes.suffix
        attributesBlock.identifier = responseBody.data.attributes.doi
//        responseBody.data.attributes.each {k,v ->
//            if(attributesBlock.containsKey(k) && attributesBlock[k] != v){
//                attributesBlock.value = v
//                // update AB
//                // update relevant MD
//            }
//            else{
//                // add to AB
//                // add to MD
//            }
//        }

        attributesBlock
    }

    Map createAttributesBlock(MultiFacetAware multiFacetAware) {

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

    Map createDraftDoiBody(Map xmlencoded, String prefix) {

        Map doiBody = '''
{
    "data": {
        "type": "dois",
        "attributes": {
            "prefix": "${prefix}",
            "xml":"${xmlencoded}"
        }
    }
}
''' as Map

        doiBody
    }

    Map createEventDoiBody(Map attributesBlock, String prefix, String suffix, String event) {

        Map doiBody = '''
{
    "data": {
        "id": "''' + prefix + '/' + suffix + '''",
        "type": "dois",
        "attributes": {
            "event":"''' + event + '''",
            "doi": "''' + prefix + '/' + suffix + '''",
            "prefix": "''' + prefix + '''",
            "suffix": "''' + suffix + '''",
            ''' + attributesBlock + '''"

        }
    }
}
'''
        doiBody
    }

    String getStatus(MultiFacetAware multiFacetAware) {
        List<Metadata> internalMetadata = metadataService.findAllByMultiFacetAwareItemIdAndNamespace(multiFacetAware.id,
                                                                                                     "${digitalObjectIdentifiersProfileProviderService.metadataNamespace}")
        internalMetadata.find {it.key = 'status'}
    }

    MultiFacetAware findMultiFacetAwareItemByDomainTypeAndId(String domainType, String multiFacetAwareItemIdString) {
        findMultiFacetAwareItemByDomainTypeAndId(domainType, UUID.fromString(multiFacetAwareItemIdString))
    }

    MultiFacetAware findMultiFacetAwareItemByDomainTypeAndId(String domainType, UUID multiFacetAwareItemId) {
        MultiFacetAwareService service = multiFacetAwareServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('DOIS02', "Facet retrieval for catalogue item [${domainType}] with no supporting service")
        service.get(multiFacetAwareItemId)
    }

    String createAndEncodeDataCiteXml(Map submissionData) {
        Base64.getEncoder().encodeToString(createDataCiteXml(submissionData).bytes)
    }

    String createDataCiteXml(Map submissionData) {
        log.debug('Exporting model using template')
        Template template = markupViewTemplateEngine.resolveTemplate('/dataCite/dataCite')

        if (!template) {
            log.error('Could not find template for XML at path {}', '/dataCite/dataCite')
            throw new ApiInternalException('DCS02', "Could not find template for XML at path [/dataCite/dataCite]")
        }
        def writable = template.make(submissionData: submissionData)
        def sw = new StringWriter()
        writable.writeTo(sw)
        sw.toString()
    }
}