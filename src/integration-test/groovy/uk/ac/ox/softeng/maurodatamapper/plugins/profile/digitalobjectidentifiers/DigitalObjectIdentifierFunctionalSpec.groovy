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
package uk.ac.ox.softeng.maurodatamapper.plugins.profile.digitalobjectidentifiers

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Rollback
import org.junit.After
import org.junit.Before
import org.mockito.Mock

class DigitalObjectIdentifierFunctionalSpec extends BaseFunctionalSpec {

    @Mock
    MetadataService metadataService

    @Mock
    CatalogueItemService catalogueItemService

    DataModel basicModel

    @Override
    String getResourcePath() {
        ''
    }

    @Before
    def setup() {
        mockDomains(Folder, Authority)
        createBasicModel()


    }

    @After
    def cleanup() {
    }


    @Rollback
    void 'DS01 Testing adding doi metadata to a dataModel'() {

    }

    createBasicModel() {

        basicModel = new DataModel()
    }

}
