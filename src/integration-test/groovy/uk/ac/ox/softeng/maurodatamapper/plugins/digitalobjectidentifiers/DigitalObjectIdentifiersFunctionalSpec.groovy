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
package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import java.nio.charset.Charset

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getFUNCTIONAL_TEST
import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getFUNCTIONAL_TEST

@Slf4j
@Integration
class DigitalObjectIdentifiersFunctionalSpec extends BaseFunctionalSpec {

    DigitalObjectIdentifiersService digitalObjectIdentifiersService

    @Shared
    String doiString

    @Shared
    UUID folderId

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        sessionFactory.currentSession.flush()
        folderId = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST).save(flush: true).id
        assert folderId

        doiString = '10.4124/kzn3hb2vh8.1'
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataModelFunctionalSpec')
        cleanUpResources(Folder, Classifier, SemanticLink)
    }

    @Override
    String getResourcePath() {
        ''
    }

    //    GET /api/doi/${digitalObjectIdentifier}
    void 'DC01 test retrieving a MultiFacetItem via DOI'() {
        given:
        String id = getStatusCheckId()

        when:
        GET("doi/$doiString")

        then:
        verifyResponse(HttpStatus.OK, response)
        assert responseBody().id == id

        cleanup:
        DELETE("dataModels/$id?permanent=true")
        verifyResponse(HttpStatus.NO_CONTENT, response)
    }

    //    GET /api/{multiFacetAwareDomainType}/{multiFacetAwareId}/doi
    void 'DC02 test retrieving a DOI via MultiFacet Item Domain and ID'() {
        given:
        String id = getStatusCheckId()

        when:
        GET("dataModels/$id/doi")

        then:
        verifyResponse(HttpStatus.OK, response)
        assert responseBody().status == 'draft'
        assert responseBody().identifier == doiString

        cleanup:
        DELETE("dataModels/$id?permanent=true")
        verifyResponse(HttpStatus.NO_CONTENT, response)

    }

    String getStatusCheckId(){
        POST("folders/$folderId/dataModels",[
            label : 'Functional Test DataModel'
        ])
        verifyResponse(HttpStatus.CREATED, response)
        String id = responseBody().id

        POST("dataModels/$id/metadata",[
            namespace: digitalObjectIdentifiersService.buildNamespaceInternal(),
            key: DigitalObjectIdentifiersService.IDENTIFIER_KEY,
            value: doiString
        ])
        verifyResponse(HttpStatus.CREATED, response)

        POST("dataModels/$id/metadata",[
            namespace: digitalObjectIdentifiersService.buildNamespaceInternal(),
            key: digitalObjectIdentifiersService.STATUS_KEY,
            value: DoiStatusEnum.DRAFT.toString()
        ])
        verifyResponse(HttpStatus.CREATED, response)

        id
    }
}
