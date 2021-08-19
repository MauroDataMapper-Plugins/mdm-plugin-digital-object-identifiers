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
        updateDoiStatus(identifier.multiFacetAwareItemId, status)
    }

    void updateDoiStatus(UUID multiFacetAwareItemId, DoiStatusEnum status) {
        Metadata statusMetadata = metadataService.findAllByMultiFacetAwareItemIdAndNamespace(multiFacetAwareItemId,
                                                                                             buildNamespaceInternal())
            .find { it.key == STATUS_KEY }
        statusMetadata.value = status
        metadataService.save(statusMetadata)
    }

    void setDoiInformation(MultiFacetAware multiFacetAwareItem, String identifier, DoiStatusEnum status, User user) {
        Metadata identifierMetadata = new Metadata(namespace: buildNamespaceInternal(),
                                                   key: IDENTIFIER_KEY,
                                                   value: identifier,
                                                   createdBy: user.getEmailAddress(),
                                                   multiFacetAwareItem: multiFacetAwareItem)
        multiFacetAwareItem.addToMetadata(identifierMetadata)
        metadataService.save(identifierMetadata)
        Metadata statusMetadata = new Metadata(namespace: buildNamespaceInternal(),
                                               key: STATUS_KEY,
                                               value: status.toString(),
                                               createdBy: user.getEmailAddress(),
                                               multiFacetAwareItem: multiFacetAwareItem)
        multiFacetAwareItem.addToMetadata(statusMetadata)
        metadataService.save(statusMetadata)
        findMultiFacetAwareService(multiFacetAwareItem.domainType).save(multiFacetAwareItem)
        sessionFactory.currentSession.flush()
    }

    Metadata findIdentifierMetadataByDoi(String doi) {
        Metadata.byNamespaceAndKey(buildNamespaceInternal(), IDENTIFIER_KEY).eq('value', doi).get()
    }

    void retireDoi(String doi) {
        updateDoiStatus(doi, DoiStatusEnum.RETIRED)
    }

    DoiStatusEnum getDoiStatus(String doi) {
        Metadata identifierMetadata = findIdentifierMetadataByDoi(doi)
        getDoiStatus(identifierMetadata.multiFacetAwareItemId)
    }

    DoiStatusEnum getDoiStatus(UUID multiFacetAwareItemId) {
        String status = metadataService.findAllByMultiFacetAwareItemIdAndNamespace(multiFacetAwareItemId, buildNamespaceInternal())
            .find { it.key == STATUS_KEY }?.value
        status ? DoiStatusEnum.findDoiStatus(status) : null
    }

    Map<String, String> findDoiInformationByMultiFacetAwareItemId(String domainType, UUID multiFacetAwareItemId) {
        List<Metadata> metadataList = metadataService.findAllByMultiFacetAwareItemIdAndNamespace(multiFacetAwareItemId, buildNamespaceInternal())
        if (!metadataList) return [:]
        [identifier: metadataList.find { it.key == IDENTIFIER_KEY }.value,
         status    : metadataList.find { it.key == STATUS_KEY }.value]
    }

    String buildNamespaceInternal() {
        "${digitalObjectIdentifiersProfileProviderService.metadataNamespace}.${INTERNAL_DOI_NAMESPACE}"
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

        updateFromResponse(responseBody, attributesBlock, multiFacetAware, user)
        setDoiInformation(multiFacetAware, attributesBlock.identifier as String, DoiStatusEnum.DRAFT, user)
    }

    def submitAsDraft(DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient, Map attributesBlock, ApiProperty prefixProperty,
                      MultiFacetAware multiFacetAware, User user) {

        String encodedXml = createAndEncodeDataCiteXml(attributesBlock)
        Map draftBody = createDoiBody(prefixProperty.value, encodedXml, attributesBlock.suffix as String)

        Map<String, Object> responseBody = digitalObjectIdentifiersServerClient.putMapToClient(draftBody,
                                                                                               "${prefixProperty.value}/${attributesBlock.suffix}")

        updateFromResponse(responseBody, attributesBlock, multiFacetAware, user)
        updateDoiStatus(multiFacetAware.id, DoiStatusEnum.DRAFT)
    }

    def submitAsRetire(DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient, Map attributesBlock, ApiProperty prefixProperty,
                       ApiProperty siteUrlProperty, MultiFacetAware multiFacetAware,
                       User user) {

        String event
        if (attributesBlock.status == 'draft') {
            event = 'register'
        } else if (attributesBlock.status == 'findable') {
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

        updateFromResponse(responseBody, attributesBlock, multiFacetAware, user)
        updateDoiStatus(multiFacetAware.id, DoiStatusEnum.RETIRED)
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

        updateFromResponse(responseBody, attributesBlock, multiFacetAware, user)
        updateDoiStatus(multiFacetAware.id, DoiStatusEnum.FINAL)
    }

    def updateFromResponse(Map<String, Object> responseBody, Map attributesBlock, MultiFacetAware multiFacetAware,
                           User user) {

        if (attributesBlock.suffix != responseBody.data.attributes.suffix) {
            attributesBlock.suffix = responseBody.data.attributes.suffix

            Metadata suffixMetadata = new Metadata(namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
                                                   key: 'suffix',
                                                   value: attributesBlock.suffix,
                                                   createdBy: user.getEmailAddress(),
                                                   multiFacetAwareItem: multiFacetAware)
            multiFacetAware.addToMetadata(suffixMetadata)
            metadataService.save(suffixMetadata)
        }
        if (attributesBlock.prefix != responseBody.data.attributes.prefix) {
            attributesBlock.prefix = responseBody.data.attributes.prefix

            Metadata prefixMetadata = new Metadata(namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
                                                   key: 'prefix',
                                                   value: attributesBlock.prefix,
                                                   createdBy: user.getEmailAddress(),
                                                   multiFacetAwareItem: multiFacetAware)
            multiFacetAware.addToMetadata(prefixMetadata)
            metadataService.save(prefixMetadata)
        }
        if (attributesBlock.identifier != responseBody.data.attributes.doi) {
            attributesBlock.identifier = responseBody.data.attributes.doi
            Metadata doiMetadata = new Metadata(namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
                                                key: 'identifier',
                                                value: attributesBlock.identifier,
                                                createdBy: user.getEmailAddress(),
                                                multiFacetAwareItem: multiFacetAware)
            multiFacetAware.addToMetadata(doiMetadata)
            metadataService.save(doiMetadata)
        }
        if (attributesBlock.status != responseBody.data.attributes.state) {
            attributesBlock.status = responseBody.data.attributes.state
            Metadata statusMetadata = new Metadata(namespace: digitalObjectIdentifiersProfileProviderService.metadataNamespace,
                                                   key: 'status',
                                                   value: attributesBlock.status,
                                                   createdBy: user.getEmailAddress(),
                                                   multiFacetAwareItem: multiFacetAware)
            multiFacetAware.addToMetadata(statusMetadata)
            metadataService.save(statusMetadata)
        }

        findMultiFacetAwareService(multiFacetAware.domainType).save(multiFacetAware)
        attributesBlock
    }

    Map createAttributesBlock(MultiFacetAware multiFacetAware) {

        def profile = digitalObjectIdentifiersProfileProviderService.createProfileFromEntity(multiFacetAware)

        List<Map<String, Object>> sectionsData = profile.sections.collect { section ->
            Map<String, Object> data = section.fields.collectEntries { field ->
                [field.metadataPropertyName, field.currentValue]
            }
            data.findAll { k, v -> v }
        }

        Map body = [:]
        sectionsData.each { sd ->
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
            endpointProperty: apiPropertyList.find { it.key == 'endpoint' },
            prefixProperty  : apiPropertyList.find { it.key == 'prefix' },
            usernameProperty: apiPropertyList.find { it.key == 'username' },
            passwordProperty: apiPropertyList.find { it.key == 'password' },
            siteUrlProperty : apiPropertyService.findByApiPropertyEnum(ApiPropertyEnum.SITE_URL),
        ]
    }

    MultiFacetAware findMultiFacetAwareItemByDomainTypeAndId(String domainType, String multiFacetAwareItemIdString) {
        findMultiFacetAwareItemByDomainTypeAndId(domainType, UUID.fromString(multiFacetAwareItemIdString))
    }

    MultiFacetAware findMultiFacetAwareItemByDomainTypeAndId(String domainType, UUID multiFacetAwareItemId) {
        MultiFacetAwareService service = multiFacetAwareServices.find { it.handles(domainType) }
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