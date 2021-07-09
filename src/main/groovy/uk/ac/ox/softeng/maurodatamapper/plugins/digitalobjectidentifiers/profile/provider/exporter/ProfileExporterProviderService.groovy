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
