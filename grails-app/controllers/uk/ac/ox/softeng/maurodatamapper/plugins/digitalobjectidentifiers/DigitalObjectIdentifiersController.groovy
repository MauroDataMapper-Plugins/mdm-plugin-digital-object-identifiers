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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
class DigitalObjectIdentifiersController implements ResourcelessMdmController {

    static responseFormats = ['json', 'xml']

    DigitalObjectIdentifiersService digitalObjectIdentifiersService

    def digitalObjectIdentifierItem() {
        log.debug('find by doi')
        MultiFacetAware instance = digitalObjectIdentifiersService.findMultiFacetAwareItemByDoi(params.digitalObjectIdentifier)
        if (!instance) return notFound(MultiFacetAware, params.digitalObjectIdentifier)
        respond(instance, [model: [userSecurityPolicyManager: currentUserSecurityPolicyManager,
                                   doiItem                  : instance],
                           view : 'digitalObjectIdentifierItem'])
    }

    def digitalObjectIdentifierInformation() {
        log.debug('find by domain and MultiFacetAware Id')
        Map<String,String> information = digitalObjectIdentifiersService.findDoiInformationByMultiFacetAwareItemId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)
        if (!information) return notFound(Metadata, params.multiFacetAwareItemId)
        respond params.multiFacetAwareItemId, [model: information, view: 'digitalObjectIdentifierInformation']

    }
    @Transactional
    def submit() {
        MultiFacetAware multiFacetAware =
            digitalObjectIdentifiersService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType,
                                                                                     params.multiFacetAwareItemId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }

        if(!params.submissionType || params.submissionType == 'final') {
            digitalObjectIdentifiersService.submitDoi(multiFacetAware, params.submissionType)
        }

        if(params.submissionType == 'draft') {
            log.error('Cannot submit {} in status draft.', params.multiFacetAwareItemId)
            throw new ApiInternalException('DP01', "Cannot submit ${params.multiFacetAwareItemId} in status draft.")
        }

    }

    @Transactional
    def delete() {
        MultiFacetAware multiFacetAware =
            digitalObjectIdentifiersService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType,
                                                                                     params.multiFacetAwareItemId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }

        digitalObjectIdentifiersService.retireDoi(multiFacetAware)

        respond multiFacetAware
    }

    protected String getProfileProviderServiceId(Map params) {
        String baseId = "${params.profileNamespace}:${params.profileName}"
        params.profileVersion ? "${baseId}:${params.profileVersion}" : baseId
    }
}
