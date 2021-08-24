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
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.profile.DigitalObjectIdentifiersProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.web.client.DigitalObjectIdentifiersServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.transactions.Transactional
import grails.plugin.markup.view.MarkupViewTemplateEngine
import groovy.text.Template
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

@Slf4j
@Transactional
class DigitalObjectIdentifiersService {

    @Autowired
    MarkupViewTemplateEngine markupViewTemplateEngine
    SessionFactory sessionFactory
    MetadataService metadataService
    DigitalObjectIdentifiersProfileProviderService digitalObjectIdentifiersProfileProviderService
    ApiPropertyService apiPropertyService

    @Autowired
    List<MultiFacetAwareService> multiFacetAwareServices

    @Autowired
    ApplicationContext applicationContext

    static final String IDENTIFIER_KEY = 'identifier'
    static final String STATUS_KEY = 'status'

    String getMetadataNamespace() {
        digitalObjectIdentifiersProfileProviderService.metadataNamespace
    }

    MultiFacetAware findMultiFacetAwareItemByDoi(String doi) {
        Metadata md = findIdentifierMetadataByDoi(doi)
        if (!md) return null
        findMultiFacetAwareService(md.multiFacetAwareItemDomainType).get(md.multiFacetAwareItemId)
    }

    Metadata findIdentifierMetadataByDoi(String doi) {
        Metadata.byNamespaceAndKey(metadataNamespace, IDENTIFIER_KEY).eq('value', doi).get()
    }

    DoiStatusEnum getDoiStatus(String doi) {
        Metadata identifierMetadata = findIdentifierMetadataByDoi(doi)
        getDoiStatus(identifierMetadata.multiFacetAwareItemId)
    }

    DoiStatusEnum getDoiStatus(UUID multiFacetAwareItemId) {
        String status = metadataService.findAllByMultiFacetAwareItemIdAndNamespace(multiFacetAwareItemId, metadataNamespace)
            .find {it.key == STATUS_KEY}?.value
        status ? DoiStatusEnum.findDoiStatus(status) : null
    }

    Map<String, String> findDoiInformationByMultiFacetAwareItemId(String domainType, UUID multiFacetAwareItemId) {
        List<Metadata> metadataList = metadataService.findAllByMultiFacetAwareItemIdAndNamespace(multiFacetAwareItemId, metadataNamespace)
        [identifier: metadataList.find {it.key == IDENTIFIER_KEY}?.value ?: '',
         status    : metadataList.find {it.key == STATUS_KEY}?.value ?: DoiStatusEnum.NOT_SUBMITTED.toString()]
    }

    MultiFacetAwareService findMultiFacetAwareService(String multiFacetAwareDomainType) {
        metadataService.findServiceForMultiFacetAwareDomainType(multiFacetAwareDomainType)
    }

    void submitDoi(MultiFacetAware multiFacetAware, String submissionType, User user) {
        DoiStatusEnum status = getDoiStatus(multiFacetAware.id)

        if (submissionType != 'retire' && status == DoiStatusEnum.FINAL) {
            throw new ApiBadRequestException('DOIS02', 'MFA already registered as finalised')
        }
        if (status == DoiStatusEnum.RETIRED) throw new ApiBadRequestException('DOIS03', 'MFA already registered as retired')

        Map<String, ApiProperty> apiPropertyMap = getRequiredApiProperties()

        DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient(
            apiPropertyMap.endpointProperty.value,
            'dois',
            applicationContext,
            apiPropertyMap.usernameProperty.value,
            apiPropertyMap.passwordProperty.value)
        Map attributesBlock = createAttributesBlock(multiFacetAware)

        if (!attributesBlock.suffix) {
            submitAsSimple(digitalObjectIdentifiersServerClient, attributesBlock, apiPropertyMap.prefixProperty, multiFacetAware, user)
        }

        switch (submissionType) {
            case 'draft':
                submitAsDraft(digitalObjectIdentifiersServerClient, attributesBlock, apiPropertyMap.prefixProperty, multiFacetAware, user)
                break
            case 'retire':
                submitAsRetire(digitalObjectIdentifiersServerClient, attributesBlock, apiPropertyMap.prefixProperty,
                               apiPropertyMap.siteUrlProperty, multiFacetAware, user)
                break
            case 'finalise':
                submitAsFinal(digitalObjectIdentifiersServerClient, attributesBlock, apiPropertyMap.prefixProperty,
                              apiPropertyMap.siteUrlProperty, multiFacetAware, user)
                break
            default:
                submitAsFinal(digitalObjectIdentifiersServerClient, attributesBlock, apiPropertyMap.prefixProperty,
                              apiPropertyMap.siteUrlProperty, multiFacetAware, user)
                break
        }
    }

    def submitAsSimple(DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient, Map attributesBlock, ApiProperty prefixProperty,
                       MultiFacetAware multiFacetAware, User user) {

        Map simpleBody = createDoiBody(prefixProperty.value)
        Map<String, Object> responseBody = digitalObjectIdentifiersServerClient.sendMapToClient(simpleBody)

        updateFromResponse(responseBody, attributesBlock, multiFacetAware, DoiStatusEnum.DRAFT, user)
    }

    def submitAsDraft(DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient, Map attributesBlock, ApiProperty prefixProperty,
                      MultiFacetAware multiFacetAware, User user) {

        String encodedXml = createAndEncodeDataCiteXml(attributesBlock)
        Map draftBody = createDoiBody(prefixProperty.value, encodedXml, attributesBlock.suffix as String)

        Map<String, Object> responseBody = digitalObjectIdentifiersServerClient.putMapToClient(draftBody,
                                                                                               "${prefixProperty.value}/${attributesBlock.suffix}")

        updateFromResponse(responseBody, attributesBlock, multiFacetAware, DoiStatusEnum.DRAFT, user)
    }

    def submitAsRetire(DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient, Map attributesBlock, ApiProperty prefixProperty,
                       ApiProperty siteUrlProperty, MultiFacetAware multiFacetAware,
                       User user) {

        String event
        if (attributesBlock.status == 'draft') {
            event = 'register'
        } else if (attributesBlock.status == 'final') {
            event = 'hide'
        } else {
            throw new ApiBadRequestException('sd02', 'Incompatible status of DOI.')
        }

        if (!siteUrlProperty) {
            throw new ApiBadRequestException('DOISXX', 'Cannot submit DOI without the Site URL being set')
        }
        if (!attributesBlock.url) {
            attributesBlock.url = "${siteUrlProperty.value}/#/doi/${prefixProperty.value}/${attributesBlock.suffix}"
        }
        String encodedXml = createAndEncodeDataCiteXml(attributesBlock)

        Map retireBody = createDoiBody(prefixProperty.value, encodedXml, attributesBlock.suffix as String, event, attributesBlock.url as String)

        Map<String, Object> responseBody = digitalObjectIdentifiersServerClient.putMapToClient(retireBody,
                                                                                               "${prefixProperty.value}/${attributesBlock.suffix}"
        )

        updateFromResponse(responseBody, attributesBlock, multiFacetAware, DoiStatusEnum.RETIRED, user)
    }

    def submitAsFinal(DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient, Map attributesBlock, ApiProperty prefixProperty,
                      ApiProperty siteUrlProperty, MultiFacetAware multiFacetAware,
                      User user) {

        String event = 'publish'
        attributesBlock.event = event

        if (!siteUrlProperty) {
            throw new ApiBadRequestException('DOISXX', 'Cannot submit DOI without the Site URL being set')
        }
        if (!attributesBlock.url) {
            attributesBlock.url = "${siteUrlProperty.value}/#/doi/${prefixProperty.value}/${attributesBlock.suffix}"
        }

        String encodedXml = createAndEncodeDataCiteXml(attributesBlock)
        Map finalBody = createDoiBody(prefixProperty.value, encodedXml, attributesBlock.suffix as String, event, attributesBlock.url as String)

        Map<String, Object> responseBody = digitalObjectIdentifiersServerClient.putMapToClient(finalBody,
                                                                                               "${prefixProperty.value}/${attributesBlock.suffix}"
        )

        updateFromResponse(responseBody, attributesBlock, multiFacetAware, DoiStatusEnum.FINAL, user)
    }

    def updateFromResponse(Map<String, Object> responseBody, Map<String, String> attributesBlock, MultiFacetAware multiFacetAware, DoiStatusEnum doiStatus, User user) {

        updateOrCreateMetadataAndAttributes(multiFacetAware, attributesBlock, 'suffix', responseBody.data.attributes.suffix, user)
        updateOrCreateMetadataAndAttributes(multiFacetAware, attributesBlock, 'prefix', responseBody.data.attributes.prefix, user)
        updateOrCreateMetadataAndAttributes(multiFacetAware, attributesBlock, 'identifier', responseBody.data.attributes.doi, user)
        updateOrCreateMetadataAndAttributes(multiFacetAware, attributesBlock, 'state', responseBody.data.attributes.state, user)
        updateOrCreateMetadataAndAttributes(multiFacetAware, attributesBlock, 'status', doiStatus.toString(), user)

        findMultiFacetAwareService(multiFacetAware.domainType).save(multiFacetAware)
        attributesBlock
    }

    void updateOrCreateMetadataAndAttributes(MultiFacetAware multiFacetAwareItem, Map attributesBlock, String field, String value, User user) {
        Metadata metadata = Metadata.byMultiFacetAwareItemIdAndNamespace(multiFacetAwareItem.id, metadataNamespace).eq('key', field).get()
        if (!metadata) {
            metadata = new Metadata(namespace: metadataNamespace,
                                    key: field,
                                    value: value,
                                    createdBy: user.getEmailAddress(),
                                    multiFacetAwareItem: multiFacetAwareItem)
            multiFacetAwareItem.addToMetadata(metadata)

        } else if (metadata.value != value) {
            metadata.value = value
        }
        // Make sure attributes block matches the value
        attributesBlock[field] = value
        metadataService.save(metadata)
    }

    Map<String, String> createAttributesBlock(MultiFacetAware multiFacetAware) {

        def profile = digitalObjectIdentifiersProfileProviderService.createProfileFromEntity(multiFacetAware)

        List<Map<String, String>> sectionsData = profile.sections.collect {section ->
            Map<String, String> data = section.fields.collectEntries {field ->
                [field.metadataPropertyName, field.currentValue]
            }
            data.findAll {k, v -> v}
        }

        Map<String, String> body = [:]
        sectionsData.each {sd ->
            body.putAll(sd)
        }

        body
    }

    Map createDoiBody(String prefix, String encodedXml = null, String suffix = null, String event = null, String url = null) {
        Map base = [
            data: [
                type      : "dois",
                attributes: [
                    prefix: prefix
                ]
            ]
        ]

        if (encodedXml) {
            base.data.attributes.xml = encodedXml
        }
        if (event) {
            base.data.attributes.event = event
        }
        if (suffix) {
            base.data.attributes.suffix = suffix
            base.data.attributes.doi = "$prefix/$suffix"
            base.data.id = "$prefix/$suffix"
        }
        if (url) {
            base.data.attributes.url = url
        }
        base
    }

    Map<String, ApiProperty> getRequiredApiProperties() {
        List<ApiProperty> apiPropertyList = ApiProperty.findAllByCategory(BootStrap.DOI_API_PROPERTY_CATEGORY)
        [
            endpointProperty: apiPropertyList.find {it.key == 'endpoint'},
            prefixProperty  : apiPropertyList.find {it.key == 'prefix'},
            usernameProperty: apiPropertyList.find {it.key == 'username'},
            passwordProperty: apiPropertyList.find {it.key == 'password'},
            siteUrlProperty : apiPropertyService.findByApiPropertyEnum(ApiPropertyEnum.SITE_URL),
        ]
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