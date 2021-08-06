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
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

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

    def retireDoi(MultiFacetAware multiFacetAware, String submissionType, User user) {

        String status = getStatus(multiFacetAware)
        if (status == 'registered') throw new ApiBadRequestException('SD01', 'MFA already registered as retired')

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

        submitAsRetire(digitalObjectIdentifiersServerClient, attributesBlock, prefixProperty, siteUrlProperty, usernameProperty, passwordProperty,
                       multiFacetAware, user)

        return multiFacetAware
    }

    def submitDoi(MultiFacetAware multiFacetAware, String submissionType, User user) {

        String status = getStatus(multiFacetAware)

        if (status == 'finalised') throw new ApiBadRequestException('sd', 'already submitted')
        if (status == 'retired') throw new ApiBadRequestException('sd', 'already retired')

        List<ApiProperty> apiPropertyList = ApiProperty.findAllByCategory(BootStrap.DOI_API_PROPERTY_CATEGORY)

        ApiProperty endpointProperty = apiPropertyList.find {it.key == 'endpoint'}
        ApiProperty prefixProperty = apiPropertyList.find {it.key == 'prefix'}
        ApiProperty usernameProperty = apiPropertyList.find {it.key == 'username'}
        ApiProperty passwordProperty = apiPropertyList.find {it.key == 'password'}

        ApiProperty siteUrlProperty =  apiPropertyService.findByApiPropertyEnum(ApiPropertyEnum.SITE_URL)

        DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient(endpointProperty.value,
                                                                                                                             "dois",
                                                                                                                             applicationContext)
        Map attributesBlock = createAttributesBlock(multiFacetAware)

        if (!attributesBlock.suffix) {
            submitAsSimple(digitalObjectIdentifiersServerClient, attributesBlock, prefixProperty, usernameProperty, passwordProperty,
                           multiFacetAware, user)
        }

        if(submissionType == 'draft'){
            submitAsDraft(digitalObjectIdentifiersServerClient, attributesBlock, prefixProperty, usernameProperty, passwordProperty,
                          multiFacetAware, user)
        }

        if(submissionType == 'retire'){
            submitAsRetire(digitalObjectIdentifiersServerClient, attributesBlock, prefixProperty, siteUrlProperty, usernameProperty,
                           passwordProperty, multiFacetAware, user)
        }

        if(submissionType == 'finalise'){
            submitAsFinal(digitalObjectIdentifiersServerClient, attributesBlock, prefixProperty, siteUrlProperty, usernameProperty,
                          passwordProperty, multiFacetAware, user)
        }
    }

    def submitAsSimple(DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient, Map attributesBlock, ApiProperty prefixProperty,
                       ApiProperty usernameProperty, ApiProperty passwordProperty, MultiFacetAware multiFacetAware, User user) {

        Map simpleBody = createDoiBody(prefixProperty.value)
        Map<String,Object> responseBody = digitalObjectIdentifiersServerClient.sendMapToClient('',
                                                                                               simpleBody,
                                                                                               usernameProperty.value,
                                                                                               passwordProperty.value)

        updateFromResponse(responseBody, attributesBlock, multiFacetAware, user)
    }

    def submitAsDraft(DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient, Map attributesBlock, ApiProperty prefixProperty,
                     ApiProperty usernameProperty, ApiProperty passwordProperty, MultiFacetAware multiFacetAware, User user) {

        String encodedXml = createAndEncodeDataCiteXml(attributesBlock)
        Map draftBody = createDoiBody(prefixProperty.value, encodedXml, attributesBlock.suffix as String)

        Map<String,Object> responseBody = digitalObjectIdentifiersServerClient.putMapToClient(prefixProperty.value + '/' + attributesBlock.suffix,
                                                                                              draftBody,
                                                                                              usernameProperty.value,
                                                                                              passwordProperty.value)

        updateFromResponse(responseBody, attributesBlock, multiFacetAware, user)
    }

    def submitAsRetire(DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient, Map attributesBlock, ApiProperty prefixProperty,
                       ApiProperty siteUrlProperty, ApiProperty usernameProperty, ApiProperty passwordProperty, MultiFacetAware multiFacetAware,
                       User user){

        String event
        if(attributesBlock.status == 'draft') {
            event = 'register'
        }
        else if (attributesBlock.status == 'findable') {
            event = 'hide'
        }
        else {
            throw new ApiBadRequestException('sd02', 'Incompatible status of DOI.')
        }


        if(!siteUrlProperty){
            throw new ApiBadRequestException('DOISXX','Cannot submit DOI without the Site URL being set')
        }
        if(!attributesBlock.url) {
            attributesBlock.url = "${siteUrlProperty.value}/#/doi/${prefixProperty.value}/${attributesBlock.suffix}"
        }
        String encodedXml = createAndEncodeDataCiteXml(attributesBlock)

        Map retireBody = createDoiBody(prefixProperty.value, encodedXml, attributesBlock.suffix as String, event, attributesBlock.url as String)

        Map<String,Object> responseBody = digitalObjectIdentifiersServerClient.putMapToClient(prefixProperty.value + '/' + attributesBlock.suffix,
                                                                                              retireBody,
                                                                                              usernameProperty.value,
                                                                                              passwordProperty.value)

        updateFromResponse(responseBody, attributesBlock, multiFacetAware, user)
    }

    def submitAsFinal(DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient, Map attributesBlock, ApiProperty prefixProperty,
                      ApiProperty siteUrlProperty, ApiProperty usernameProperty, ApiProperty passwordProperty, MultiFacetAware multiFacetAware,
                      User user){

        String event = 'publish'
        attributesBlock.event = event

        if(!siteUrlProperty){
            throw new ApiBadRequestException('DOISXX','Cannot submit DOI without the Site URL being set')
        }
        if(!attributesBlock.url) {
            attributesBlock.url = "${siteUrlProperty.value}/#/doi/${prefixProperty.value}/${attributesBlock.suffix}"
        }

        String encodedXml = createAndEncodeDataCiteXml(attributesBlock)
        Map finalBody = createDoiBody(prefixProperty.value, encodedXml, attributesBlock.suffix as String, event, attributesBlock.url as String)

        Map<String,Object> responseBody = digitalObjectIdentifiersServerClient.putMapToClient(prefixProperty.value + '/' + attributesBlock.suffix,
                                                                                              finalBody,
                                                                                              usernameProperty.value,
                                                                                              passwordProperty.value)

        updateFromResponse(responseBody, attributesBlock)
    }

    def updateFromResponse(Map<String,Object> responseBody, Map attributesBlock, MultiFacetAware multiFacetAware,
                           User user){

        if(!attributesBlock.suffix){
            attributesBlock.suffix = responseBody.data.attributes.suffix
            log.debug(attributesBlock.suffix)
            Metadata suffixMetadata = new Metadata(namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
                                                   key: 'suffix',
                                                   value: attributesBlock.suffix,
                                                   createdBy: user.getEmailAddress())
            multiFacetAware.addToMetadata(suffixMetadata)
            metadataService.addFacetAndSaveMultiFacetAware('metadata', suffixMetadata)
        }
        if(!attributesBlock.identifier){
            attributesBlock.identifier = responseBody.data.attributes.doi
            Metadata doiMetadata = new Metadata(namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
                                                key: 'identifier',
                                                value: attributesBlock.identifier,
                                                createdBy: user.getEmailAddress())
            multiFacetAware.addToMetadata(doiMetadata)
            metadataService.addFacetAndSaveMultiFacetAware('metadata', doiMetadata)
        }

        attributesBlock
    }

    Map createAttributesBlock(MultiFacetAware multiFacetAware) {

        def profile = digitalObjectIdentifiersProfileProviderService.createProfileFromEntity(multiFacetAware)

        List<Map<String, Object>> sectionsData = profile.sections.collect {section ->
            Map<String, Object> data = section.fields.collectEntries {field ->
                [field.metadataPropertyName, field.currentValue]
            }
            data.findAll {k, v -> v}
        }

        Map body = [:]
        sectionsData.each {sd ->
            body.putAll(sd)
        }

        body
    }

    Map createDoiBody(String prefix, String encodedXml=null, String suffix=null, String event=null, String url=null) {
        Map base =  [
            data: [
                type: "dois",
                attributes: [
                    prefix: prefix
                ]
            ]
        ]

        if(encodedXml) {
            base.data.attributes.xml = encodedXml
        }
        if(event) {
            base.data.attributes.event = event
        }
        if(suffix){
            base.data.attributes.suffix = suffix
            base.data.attributes.doi =  "$prefix/$suffix"
            base.data.id = "$prefix/$suffix"
        }
        if(url){
            base.data.attributes.url = url
        }
        base
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