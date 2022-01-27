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
package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.web.client

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.core.GrailsApplication
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import spock.lang.Shared

@Slf4j
@Integration
class DigitalObjectIdentifiersServerClientSpec extends BaseIntegrationSpec {
    @Shared
    DigitalObjectIdentifiersServerClient digitalObjectIdentifiersServerClient

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    ApplicationContext applicationContext

    @Shared
    String username
    @Shared
    String password

    @Shared
    String prefix

    @Shared
    Map body

    @OnceBefore
    void configureClient() {
        Map props = grailsApplication.config.maurodatamapper.digitalobjectidentifiers
        assert props
        assert props.username
        assert props.password
        assert props.prefix == '10.80079'
        assert props.endpoint == 'https://api.test.datacite.org'
        prefix = props.prefix
        username = props.username
        password = props.password

        digitalObjectIdentifiersServerClient = new DigitalObjectIdentifiersServerClient(props.endpoint as String,
                                                                                        'dois',
                                                                                        applicationContext,
                                                                                        username, password)

        body = [
            data: [
                type      : "dois",
                attributes: [
                    prefix: props.prefix
                ]
            ]
        ]
    }

    @Override
    void setupDomainData() {
    }

    void 'DOI-01: Test posting to DataCite to get suffix'() {
        when:
        Map<String, Object> response = digitalObjectIdentifiersServerClient.sendMapToClient(body,)

        then:
        response
        response.data.attributes.suffix
        response.data.attributes.state == 'draft'
    }

    void 'DOI-02: Test posting to DataCite without usr/pwd authentication'() {
        when:
        digitalObjectIdentifiersServerClient.updateCredentials('', '')
        digitalObjectIdentifiersServerClient.sendMapToClient(body)

        then:
        ApiBadRequestException ex = thrown(ApiBadRequestException)
        ex.status.code == 400
        ex.message.startsWith('Requested endpoint could not be found')

        cleanup:
        digitalObjectIdentifiersServerClient.updateCredentials(username, password)
    }

    void 'DOI-03: Test posting to DataCite with unauthorised DOI prefix'() {
        when:
        body.data.attributes.prefix = '10.0000'
        digitalObjectIdentifiersServerClient.sendMapToClient(body)

        then:
        ApiInternalException ex = thrown(ApiInternalException)
        ex.cause.response.status.code == 403

        cleanup:
        body.data.attributes.prefix = prefix
    }
}
