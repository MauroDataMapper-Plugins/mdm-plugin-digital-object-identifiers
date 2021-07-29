Map<String, Object> sd = submissionData as Map<String, Object>

xmlDeclaration()

'resource'(xmlns: "http://datacite.org/schema/kernel-4",
           'xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance",
           'xsi:schemaLocation': "http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4.1/metadata.xsd") {
    'identifier'(sd.Identifiers, identifierType: 'DOI', sd.identifier)
    'creators' {
        'creator' {
            ('creatorName'(sd."Creator Name"))
        }
    }
//    'creators' {
//        sd.creators.each { cr ->
//            'creator' {
//                'creatorName'(cr.creatorName)
//            }
//        }
//    }

//    'titles' {
//        sd.titles.each { ti ->
//            'title'(ti.title)
//        }
//    }
    'titles' {
        'title'(sd.Title)
    }
    'publisher'(sd.Publisher)
    'publicationYear'(sd."Publication Year")
    'resourceType'(sd.resourceType, resourceTypeGeneral: sd."Resource Type")
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