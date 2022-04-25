/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.DoiStatusEnum
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.grails.core.artefact.DomainClassArtefactHandler
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class DigitalObjectIdentifiersProfileProviderService extends JsonProfileProviderService {

    @Autowired
    GrailsApplication grailsApplication

    ApiPropertyService apiPropertyService

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
        //TODO update to use the full profile which needs to be completed following overhaul of profiles to allow multiple sections
        'DataCiteDigitalObjectIdentifiersProfile-4.4-partial.json'
    }

    @Override
    Boolean canBeEditedAfterFinalisation() {
        true
    }

    @Override
    List<String> profileApplicableForDomains() {
        grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE)
            .findAll {MetadataAware.isAssignableFrom(it.clazz) && !it.isAbstract()}
            .collect {grailsClass -> grailsClass.getName()}
    }

    @Override
    JsonProfile updateUneditableFields(JsonProfile profile) {
        // Make sure the prefix field is populated.
        // The other fixed fields will be populated once submissions start to flow
        ApiProperty prefixProperty = apiPropertyService.findByKey('prefix')
        ProfileSection fixedSection = profile.sections.find {it.name == 'Predefined/Supplied Fields'}
        findAndSetProfileField(fixedSection, 'prefix', prefixProperty.value)
        findAndSetProfileField(fixedSection, 'status', DoiStatusEnum.NOT_SUBMITTED.toString())
        fixedSection.fields.each {field ->
            if(!field.currentValue) field.currentValue = ''
        }

        profile
    }
}