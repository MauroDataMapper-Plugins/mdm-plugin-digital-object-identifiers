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

import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.AnnotationMetadataResolver
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.http.client.LoadBalancer
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.netty.DefaultHttpClient
import io.micronaut.http.client.netty.ssl.NettyClientSslBuilder
import io.micronaut.http.codec.MediaTypeCodecRegistry
import io.micronaut.http.exceptions.HttpException
import io.micronaut.http.uri.UriBuilder
import io.netty.channel.MultithreadEventLoopGroup
import io.netty.util.concurrent.DefaultThreadFactory
import org.springframework.context.ApplicationContext

import java.util.concurrent.ThreadFactory

@Slf4j
class DigitalObjectIdentifiersServerClient {

    private HttpClient client
    private String hostUrl
    private String contextPath
    private String username
    private String password

    DigitalObjectIdentifiersServerClient(String hostUrl, String contextPath, ApplicationContext applicationContext, String username,
                                         String password) {
        this(hostUrl, contextPath,
             applicationContext.getBean(HttpClientConfiguration),
             new DefaultThreadFactory(MultithreadEventLoopGroup),
             applicationContext.getBean(NettyClientSslBuilder),
             applicationContext.getBean(MediaTypeCodecRegistry),
             username,
             password
        )
    }

    DigitalObjectIdentifiersServerClient(String hostUrl, String contextPath,
                                         HttpClientConfiguration httpClientConfiguration,
                                         ThreadFactory threadFactory,
                                         NettyClientSslBuilder nettyClientSslBuilder,
                                         MediaTypeCodecRegistry mediaTypeCodecRegistry, String username, String password) {
        this.hostUrl = hostUrl
        this.contextPath = contextPath
        this.username = username
        this.password = password
        client = new DefaultHttpClient(LoadBalancer.fixed(hostUrl.toURL().toURI()),
                                       httpClientConfiguration,
                                       this.contextPath,
                                       threadFactory,
                                       nettyClientSslBuilder,
                                       mediaTypeCodecRegistry,
                                       AnnotationMetadataResolver.DEFAULT,
                                       Collections.emptyList())
        log.debug('Client created to connect to {}', hostUrl)
    }

    void updateCredentials(String username, String password) {
        this.username = username
        this.password = password
    }

    Map<String, Object> sendMapToClient(Map body, String url = '') {
        MutableHttpRequest<Map<String, Object>> request = HttpRequest.POST(UriBuilder.of(url).build(), body)
        makeRequestToClient(request, url)
    }

    Map<String, Object> putMapToClient(Map body, String url = '') {
        MutableHttpRequest<Map<String, Object>> request = HttpRequest.PUT(UriBuilder.of(url).build(), body)
        makeRequestToClient(request, url)
    }

    Map<String, Object> getMapFromClient(String url = '') {
        MutableHttpRequest<Map<String, Object>> request = HttpRequest.GET(UriBuilder.of(url).build())
        makeRequestToClient(request, url)
    }

    Map<String, Object> makeRequestToClient(MutableHttpRequest<Map<String, Object>> request, String url) {
        try {
            client.toBlocking().retrieve(request.basicAuth(username, password),
                                                                     Argument.of(Map, String, Object))
        }
        catch (HttpClientResponseException responseException) {
            String fullUrl = UriBuilder.of(hostUrl).path(contextPath).path(url).toString()
            if (responseException.status == HttpStatus.NOT_FOUND) {
                throw new ApiBadRequestException('DOIC01', "Requested endpoint could not be found ${fullUrl}")
            }
            throw new ApiInternalException('DOIC02', "Could not load resource from endpoint [${fullUrl}].\n" +
                                                     "Response body [${responseException.response.body()}]",
                                           responseException)
        } catch (HttpException ex) {
            String fullUrl = UriBuilder.of(hostUrl).path(contextPath).path(url).toString()
            throw new ApiInternalException('DOIC03', "Could not load resource from endpoint [${fullUrl}]", ex)
        }
    }
}
