Map<String, Object> sd = submissionData as Map<String, Object>

xmlDeclaration()

'resource'(xmlns: "http://datacite.org/schema/kernel-4",
           'xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance",
           'xsi:schemaLocation': "http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4.1/metadata.xsd") {
    'identifier'(sd.Identifiers, identifierType: 'DOI', sd.identifier)
    'creators' {
        'creator' {
            ('creatorName'(sd."creators/creator/creatorName"))
        }
    }
    'titles' {
        'title'(sd."titles/title" )
    }
    'publisher'(sd.publisher)
    'publicationYear'(sd.publicationYear)
    'resourceType'(sd.resourceType, resourceTypeGeneral: sd.resourceType)
    if (sd.descriptions) {
        'descriptions' {
            sd.descriptions.each { de ->
                'description'(de.description, descriptionType: "Other")
            }
        }
    }
//    if (sd.subjects) {
//        'subjects' {
//            sd.subjects.each { su ->
//                'subject'(su.subject)
//            }
//        }
//    }
//    if (sd.dates) {
//        'dates' {
//            sd.dates.each { da ->
//                'date'(da.date, dataType: '')
//            }
//        }
//    }
//    if (sd.relatedIdentifiers) {
//        'relatedIdentifiers' {
//            sd.relatedIdentifiers.each { re ->
//                'relatedIdentifier'(re.relatedIdentifier)
//            }
//        }
//    }
}