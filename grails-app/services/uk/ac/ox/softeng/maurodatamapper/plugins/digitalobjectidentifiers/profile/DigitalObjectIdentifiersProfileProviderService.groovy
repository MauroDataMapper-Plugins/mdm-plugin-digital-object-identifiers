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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.core.GrailsApplication
import grails.plugin.json.view.JsonViewTemplateEngine
import groovy.text.Template
import groovy.util.logging.Slf4j
import org.grails.core.artefact.DomainClassArtefactHandler
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class DigitalObjectIdentifiersProfileProviderService extends JsonProfileProviderService {

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    JsonViewTemplateEngine templateEngine

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


    //separate profile service? with FileType, FileExtension
    @Override
    ByteArrayOutputStream exportProfile(User currentUser, Profile profile) throws ApiException {
        exportModel(profile, fileType)
    }

    ByteArrayOutputStream exportModel(Profile profile, String format) {
        Template template = templateEngine.resolveTemplate(exportViewPath)

        if (!template) {
            log.error('Could not find template for format {} at path {}', format, exportViewPath)
            throw new ApiInternalException('PSE02', "Could not find template for format ${format} at path ${exportViewPath}")
        }

        def writable = template.make(profile: profile)
        def sw = new StringWriter()
        writable.writeTo(sw)
        ByteArrayOutputStream os = new ByteArrayOutputStream()
        os.write(sw.toString().bytes)
        os
    }

    @Override
    ByteArrayOutputStream exportProfiles(User currentUser, List<Profile> profiles) throws ApiException {
        throw new ApiBadRequestException('PSE01', "${getName()} cannot export multiple profiles")
    }

    static String getExportViewPath() {
        '/profile/export'
    }
}