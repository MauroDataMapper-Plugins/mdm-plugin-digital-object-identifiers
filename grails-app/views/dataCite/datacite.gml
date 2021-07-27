Map<String,Object> sd = submissionData as Map<String,Object>

'resource' {
    if (sd.identifier) 'identifier'(sd.identifier)
    if (sd.creators) {
        'creators' {
            sd.creators.each { cr ->
                'creator' {
                    'creatorName'(cr.creatorName)
                }
            }
        }
    }
    if (sd.titles) {
        'titles' {
            sd.titles.each { ti ->
                'title'(ti.title)
            }
        }
    }
    if (sd.publisher) 'publisher'(sd.publisher)
    if (sd.publicationYear) 'publicationYear'(sd.publicationYear)
    if (sd.subjects) {
        'subjects' {
            sd.subjects.each { su ->
                'subject'(su.subject)
            }
        }
    }
    if (sd.dates) {
        'dates' {
            sd.dates.each { da ->
                'date'(da.date)
            }
        }
    }
    if (sd.resourceType) 'resourceType'(sd.resourceType)
    if (sd.rightsList) {
        'rightsList' {
            sd.rightsList.each { ri ->
                'rights'(ri.rights)
            }
        }
    }
    if (sd.descriptions) {
        'descriptions' {
            sd.descriptions.each { de ->
                'description' (de.description)
    }
    if (sd.relatedIdentifiers) {
        'relatedIdentifiers' {
            sd.relatedIdentifiers.each { re ->
                'relatedIdentifier' (re.relatedIdentifier)
            }
        }
    }
}