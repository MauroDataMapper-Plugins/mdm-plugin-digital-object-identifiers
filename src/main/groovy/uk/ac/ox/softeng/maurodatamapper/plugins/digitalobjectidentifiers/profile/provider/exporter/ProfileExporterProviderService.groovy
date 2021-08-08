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
package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.profile.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileService
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.security.User

import org.springframework.beans.factory.annotation.Autowired

abstract class ProfileExporterProviderService extends ExporterProviderService {

    @Autowired
    ProfileService profileService

    abstract ByteArrayOutputStream exportDoiProfile(User currentUser, Profile profile) throws ApiException

    abstract ByteArrayOutputStream exportDoiProfiles(User currentUser, List<Profile> profiles) throws ApiException

    @Override
    ByteArrayOutputStream exportDomain(User currentUser, UUID domainId) throws ApiException {
        Profile profile = profileService.get(domainId)
        if (!profile) {
            log.error('Cannot find model id [{}] to export', domainId)
            throw new ApiInternalException('PEP01', "Cannot find model id [${domainId}] to export")
        }
        exportDoiProfile(currentUser, profile)
    }

    @Override
    ByteArrayOutputStream exportDomains(User currentUser, List<UUID> domainIds) throws ApiException {
        List<Profile> profiles = []
        List<UUID> cannotExport = []
        domainIds.each {
            Profile profile = profileService.get(it)
            if (!profile) {
                cannotExport.add it
            } else profiles.add profile
        }
        log.warn('Cannot find model ids [{}] to export', cannotExport)
        exportDoiProfiles(currentUser, profiles)
    }

    @Override
    Boolean canExportMultipleDomains() {
        false
    }

    @Override
    String getProviderType() {
        "Profile${ProviderType.EXPORTER.name}"
    }
}
