package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.profile.DigitalObjectIdentifiersProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.core.GrailsApplication
import org.springframework.context.MessageSource

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.ADMIN

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
    MessageSource messageSource
    GrailsApplication grailsApplication

    DigitalObjectIdentifiersProfileProviderService digitalObjectIdentifiersProfileProviderService
    static final List<String> KNOWN_KEYS = ['username', 'password', 'prefix', 'endpoint']
    static final String DOI_API_PROPERTY_CATEGORY = 'Digital Object Identifier Properties'

    def init = {servletContext ->
        ApiProperty.withNewTransaction {

            List<String> existingKeys = ApiProperty.findAllByCategory(DOI_API_PROPERTY_CATEGORY).collect{it.key}

            List<ApiProperty> loaded = grailsApplication.config.maurodatamapper.digitalobjectidentifiers.collect {
                new ApiProperty(key: it.key, value: it.value,
                                createdBy: ADMIN,
                                category: DOI_API_PROPERTY_CATEGORY)
            }

            KNOWN_KEYS.each {k ->
                if (!(k in loaded.collect {it.key})) {
                    loaded.add(new ApiProperty(key: k, value: 'NOT_SET',
                                                      createdBy: ADMIN,
                                                      category: DOI_API_PROPERTY_CATEGORY))
                }
            }

            // Dont override already loaded values
            ApiProperty.saveAll(loaded.findAll {!(it.key in existingKeys)})
        }
    }

    def destroy = {
    }

}

