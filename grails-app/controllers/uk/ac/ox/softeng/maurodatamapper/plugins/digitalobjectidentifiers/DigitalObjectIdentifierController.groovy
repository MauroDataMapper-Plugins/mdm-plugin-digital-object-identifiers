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

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.digitalobjectidentifiers.DigitalObjectIdentifierService

import groovy.util.logging.Slf4j

@Slf4j
class DigitalObjectIdentifierController implements ResourcelessMdmController {

    static responseFormats = ['json', 'xml']

    DigitalObjectIdentifierService digitalObjectIdentifierService

    def getMultiFacetAwareItemByDoi() {
        log.debug('find by doi')
        MultiFacetAware instance = digitalObjectIdentifierService.getMultiFacetAwareItemByDoi(params.digitalObjectIdentifier)
        if (!instance) return notFound(MultiFacetAware, params.digitalObjectIdentifier)
        respond instance
    }


    def show() {
        log.debug('find by domain and MultiFacetAware Id')
        List<Metadata> instance = digitalObjectIdentifierService.getByMultiFacetAwareDomainTypeAndId(params.multiFacetAwareDomainType, params
            .multiFacetAwareId)
        if (instance.empty) return notFound(Metadata, params.multiFacetAwareId)
        String doi = instance.find { it.key == digitalObjectIdentifierService.IDENTIFIER_KEY }.value
        String status = instance.find { it.key == digitalObjectIdentifierService.STATUS_KEY }.value
        respond "identifier": doi,
                "status": status

    }


}
