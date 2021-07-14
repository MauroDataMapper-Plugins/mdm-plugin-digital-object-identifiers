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
package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.profile

import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.grails.core.artefact.DomainClassArtefactHandler
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class DigitalObjectIdentifiersProfileProviderService extends JsonProfileProviderService {

    @Autowired
    GrailsApplication grailsApplication

    @Override
    String getMetadataNamespace() {
        'org.datacite'
    }

    @Override
    String getDisplayName() {
        'Digital Object Identifiers DataCite Dataset Schema'
    }

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }

    @Override
    String getJsonResourceFile() {
        'DataCiteDigitalObjectIdentifiersProfile.json'
    }

    @Override
    List<String> profileApplicableForDomains() {
        grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE)
            .findAll {MetadataAware.isAssignableFrom(it.clazz) && !it.isAbstract()}
            .collect {grailsClass -> grailsClass.getName()}
    }
}