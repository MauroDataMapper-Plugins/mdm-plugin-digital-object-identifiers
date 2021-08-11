Map<String, Object> sd = submissionData as Map<String, Object>

xmlDeclaration()

'resource'(xmlns: "http://datacite.org/schema/kernel-4",
           'xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance",
           'xsi:schemaLocation': "http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4.1/metadata.xsd") {
    'identifier'(sd.identifier, identifierType: 'DOI' )
    'creators' {
        'creator' {
            ('creatorName'(sd."creators/creator/creatorName"))
            if(sd."creators/creator/affiliation") {
                ('affiliation'(sd."creators/creator/affiliation"))
            }
            if(sd."creators/creator/nameIdentifier") {
                ('nameIdentifier'(sd."creators/creator/nameIdentifier"))
            }
        }
    }
    'titles' {
        'title'(sd."titles/title" )
    }
    'publisher'(sd.publisher)
    'publicationYear'(sd.publicationYear)
    'resourceType'(sd.resourceType, resourceTypeGeneral: sd.resourceType)
    if (sd."subjects/subject") {
        'subjects' {
            sd."subjects/subject".each {
                'subject'(sd."subjects/subject")
            }
        }
    }
    if (sd."contributors/contributor") {
        'contributors' {
            'contributor' {
                sd."contributors/contributor".each {
                    if(sd."contributors/contributor/contributorName") {
                        ('contributorName'(sd."contributors/contributor/contributorName"))
                    }
                    if(sd."contributors/contributor/nameIdentifier") {
                        ('nameIdentifier'(sd."contributors/contributor/nameIdentifier"))
                    }
                    if(sd."creators/creator/affiliation") {
                        ('affiliation'(sd."contributors/contributor/affiliation"))
                    }
                }
            }
        }
    }
    if (sd."dates/date") {
        'dates' {
            'date'(sd."dates/date")
        }
    }
    if (sd.language) {
        'language'(sd.language)
    }
    if (sd."alternateIdentifiers/alternateIdentifier") {
        'alternateIdentifiers' {
            'alternateIdentifier'(sd."alternateIdentifiers/alternateIdentifier")
        }
    }
    if (sd."relatedIdentifiers/relatedIdentifier") {
        'relatedIdentifiers' {
            'relatedIdentifier'(sd."relatedIdentifiers/relatedIdentifier")
        }
    }
    if (sd."sizes/size") {
        'sizes' {
            'size'(sd."sizes/size")
        }
    }
    if (sd."formats/format") {
        'formats' {
            'format'(sd."formats/format")
        }
    }
    if (sd.version) {
        'version'(sd.version)
    }
    if (sd."rightsList/rights") {
        'rightsList' {
            'rights'(sd."rightsList/rights")
        }
    }

    if (sd.descriptions) {
        'descriptions' {
            sd.descriptions.each { de ->
                'description'(de.description, descriptionType: "Other")
            }
        }
    }
}