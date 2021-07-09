package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.profile.DigitalObjectIdentifiersProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileService
import uk.ac.ox.softeng.maurodatamapper.profile.provider.ProfileProviderService

import grails.gorm.transactions.Transactional

class DigitalObjectIdentifiersProfileController implements ResourcelessMdmController {
    static responseFormats = ['json', 'xml']

    ProfileService profileService
    MetadataService metadataService
    DigitalObjectIdentifiersProfileProviderService digitalObjectIdentifiersProfileProviderService

    def submit() {
        MultiFacetAware multiFacetAware =
            profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }

        //TODO separate error messages for draft and retired?
        //TODO doiStatusEnum may need changing
        if (doiStatusKey != 'finalised') {
            log.error('Cannot submit {} in status {}', profile, doiStatusKey)
            throw new ApiInternalException('DP01', "Cannot submit ${profile} in status ${doiStatusKey}")
        }

        if (doiStatusKey == 'finalised') {
            //convert to JSON
            //submitToDataCite
        }

    }

    @Transactional
    def delete() {
        MultiFacetAware multiFacetAware =
            profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                                                                  params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        Set<Metadata> mds =
            multiFacetAware.metadata
                .findAll{ it.namespace == profileProviderService.metadataNamespace }

        mds.each {md ->
            //multiFacetAware.metadata.remove(md)
            metadataService.delete(md, true)
            metadataService.addDeletedEditToMultiFacetAwareItem(currentUser, md, params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)}
    }

    protected String getProfileProviderServiceId(Map params) {
        String baseId = "${params.profileNamespace}:${params.profileName}"
        params.profileVersion ? "${baseId}:${params.profileVersion}" : baseId
    }
}
