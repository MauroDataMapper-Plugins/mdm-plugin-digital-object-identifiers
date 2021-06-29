package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import asset.pipeline.grails.AssetResourceLocator
import grails.core.GrailsApplication
import org.springframework.core.io.Resource
import org.yaml.snakeyaml.Yaml

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.DEVELOPMENT

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


class BootStrap {
    GrailsApplication grailsApplication
    static final Map staticProperties = [apiKey: "", apiUsername: "", apiPassword: "", prefix: "", endpoint: ""]

    def init = { servletContext ->
        loadDefaultDOIProperties()
    }

    def destroy = {
    }


    void loadDefaultDOIProperties() {
        Map ymlProperties = grailsApplication.config.maurodatamapper.digitalobjectidentifiers

        if (ymlProperties.collect { it.key }.containsAll(staticProperties.keySet())) {
            loadProperties(ymlProperties)
        } else { log.warn('Missing properties found in configuration, loading them from static defaults') }
        Map mergedMap = staticProperties + ymlProperties
        loadProperties(mergedMap)
    }

    void loadProperties(Map propertiesMap) {
        List<ApiProperty> DOIProperties = propertiesMap
            .collect {
                new ApiProperty(key: it.key, value: it.value ?: 'NOT SET',
                                createdBy: DEVELOPMENT,
                                category: 'Digital Object Identifier Properties')
            }
        ApiProperty.saveAll(DOIProperties)
    }


}

