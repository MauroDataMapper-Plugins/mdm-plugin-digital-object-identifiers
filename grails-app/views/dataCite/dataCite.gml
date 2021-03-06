Map<String, Object> sd = submissionData as Map<String, Object>

xmlDeclaration()
resource(xmlns: 'http://datacite.org/schema/kernel-4',
         'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
         'xsi:schemaLocation': 'http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd') {
    identifier(sd.identifier, identifierType: 'DOI')
    creators {
        creator {
            creatorName(sd.'creators/creator/creatorName', getAttrs(sd, 'nameType', 'creators/creator/creatorNameType'))
            if (sd.'creators/creator/givenName') givenName(sd.'creators/creator/givenName')
            if (sd.'creators/creator/familyName') familyName(sd.'creators/creator/familyName')
            if (sd.'creators/creator/nameIdentifier') nameIdentifier(sd.'creators/creator/nameIdentifier')
            if (sd.'creators/creator/affiliation') affiliation(sd.'creators/creator/affiliation')
        }
    }
    titles {
        title(sd.'titles/mainTitle')
        if (sd.'titles/title') title(sd.'titles/title', getAttrs(sd, 'titleType', 'titles/titleType', 'AlternativeTitle'))
    }
    publisher(sd.publisher)
    publicationYear(sd.publicationYear)
    resourceType(sd.resourceType, resourceTypeGeneral: sd.resourceType)
    if (sd.language) language(sd.language)
    if (sd.version) version(sd.version)
    contributors {
        if (sd.'contributors/contributor/contributorName') {
            contributor(getAttrs(sd, 'contributorType', 'contributors/contributor/contributorType', 'Other')) {
                contributorName(sd.'contributors/contributor/contributorName', getAttrs(sd, 'nameType', 'contributors/contributor/contributorNameType'))
                if (sd.'contributors/contributor/givenName') givenName(sd.'contributors/contributor/givenName')
                if (sd.'contributors/contributor/familyName') familyName(sd.'contributors/contributor/familyName')
                if (sd.'contributors/contributor/nameIdentifier') nameIdentifier(sd.'contributors/contributor/nameIdentifier')
                if (sd.'contributors/contributor/affiliation') affiliation(sd.'contributors/contributor/affiliation')
            }
        }
    }
    descriptions {
        if (sd.'descriptions/mainDescription') description(sd.'descriptions/mainDescription', descriptionType: 'Abstract')
        if (sd.'descriptions/description') description(sd.'descriptions/description', getAttrs(sd, 'descriptionType', 'descriptions/descriptionType', 'Other'))
    }

    //    if (sd.'rightsList/rights') {
    //        rightsList {
    //            rights(sd.'rightsList/rights')
    //        }
    //    }
    //    if (sd.'alternateIdentifiers/alternateIdentifier') {
    //        alternateIdentifiers {
    //            alternateIdentifier(sd.'alternateIdentifiers/alternateIdentifier')
    //        }
    //    }
    //    if (sd.'relatedIdentifiers/relatedIdentifier') {
    //        relatedIdentifiers {
    //            relatedIdentifier(sd.'relatedIdentifiers/relatedIdentifier')
    //        }
    //    }
    //    if (sd.'sizes/size') {
    //        sizes {
    //            size(sd.'sizes/size')
    //        }
    //    }
    //    if (sd.'formats/format') {
    //        formats {
    //            format(sd.'formats/format')
    //        }
    //    }
    //    if (sd.'dates/date') {
    //        dates {
    //            date(sd.'dates/date')
    //        }
    //    }
    //    if (sd.'subjects/subject') {
    //        subjects {
    //            List subjects = sd.'subjects/subject'.tokenize(',')
    //            subjects.each {val ->
    //                subject(val)
    //            }
    //        }
    //    }
    //    if (sd.'fundingReferences/fundingReference/funderName') {
    //        fundingReferences {
    //            fundingReference {
    //                funderName(sd.'fundingReferences/fundingReference/funderName')
    //                if (sd.'fundingReferences/fundingReference/fundingIdentifier') {
    //                    fundingIdentifier(sd.'fundingReferences/fundingReference/fundingIdentifier')
    //                }
    //                if (sd.'fundingReferences/fundingReference/awardNumber') {
    //                    awardNumber(sd.'fundingReferences/fundingReference/awardNumber')
    //                }
    //                if (sd.'fundingReferences/fundingReference/awardTitle') {
    //                    awardTitle(sd.'fundingReferences/fundingReference/awardTitle')
    //                }
    //            }
    //        }
    //    }
}

Map getAttrs(Map sd, String attrName, String field, String defaultValue = null) {
    Map m = [:]
    if (sd.containsKey(field)) {
        m[attrName] = sd[field]
    } else if (defaultValue) {
        m[attrName] = defaultValue
    }
    m
}