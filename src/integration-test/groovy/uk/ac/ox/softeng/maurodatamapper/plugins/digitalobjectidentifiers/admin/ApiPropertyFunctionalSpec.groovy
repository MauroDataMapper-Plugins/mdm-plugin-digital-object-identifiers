/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.admin

import uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.BootStrap
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus

/**
 * @since 30/06/2021
 */
@Slf4j
@Integration
class ApiPropertyFunctionalSpec extends BaseFunctionalSpec{
    @Override
    String getResourcePath() {
        'admin/properties'
    }

    void 'test getting api properties to check doi properties are loaded'(){
        when:
        GET('')

        then:
        verifyResponse(HttpStatus.OK, response)

        when:
        List<Map> props = responseBody().items.findAll { it.category == BootStrap.DOI_API_PROPERTY_CATEGORY }

        then:
        props
        props.size() == 4
        props.find { it.key == 'prefix' && it.value == '10.80079' }
        props.find { it.key == 'endpoint' && it.value == 'https://api.test.datacite.org' }
        props.find { it.key == 'username' }
        props.find { it.key == 'password' }
    }
}
