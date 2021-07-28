package uk.ac.ox.softeng.maurodatamapper.plugins.digitalobjectidentifiers.web.client

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException

import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.AnnotationMetadataResolver
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.DefaultHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.http.client.LoadBalancer
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.ssl.NettyClientSslBuilder
import io.micronaut.http.codec.MediaTypeCodecRegistry
import io.micronaut.http.exceptions.HttpException
import io.micronaut.http.uri.UriBuilder
import io.netty.channel.MultithreadEventLoopGroup
import io.netty.util.concurrent.DefaultThreadFactory
import io.reactivex.Flowable
import org.springframework.context.ApplicationContext

import java.util.concurrent.ThreadFactory

@Slf4j
class DigitalObjectIdentifiersServerClient {

    private HttpClient client
    private String hostUrl
    private String contextPath

    DigitalObjectIdentifiersServerClient(String hostUrl, ApplicationContext applicationContext) {
        this(hostUrl, null,
             applicationContext.getBean(HttpClientConfiguration),
             new DefaultThreadFactory(MultithreadEventLoopGroup),
             applicationContext.getBean(NettyClientSslBuilder),
             applicationContext.getBean(MediaTypeCodecRegistry)
        )
    }

    DigitalObjectIdentifiersServerClient(String hostUrl, String contextPath, ApplicationContext applicationContext) {
        this(hostUrl, contextPath,
             applicationContext.getBean(HttpClientConfiguration),
             new DefaultThreadFactory(MultithreadEventLoopGroup),
             applicationContext.getBean(NettyClientSslBuilder),
             applicationContext.getBean(MediaTypeCodecRegistry)
        )
    }

    DigitalObjectIdentifiersServerClient(String hostUrl, String contextPath,
                                         HttpClientConfiguration httpClientConfiguration,
                                         ThreadFactory threadFactory,
                                         NettyClientSslBuilder nettyClientSslBuilder,
                                         MediaTypeCodecRegistry mediaTypeCodecRegistry) {
        this.hostUrl = hostUrl
        this.contextPath = contextPath
        client = new DefaultHttpClient(LoadBalancer.fixed(hostUrl.toURL()),
                                       httpClientConfiguration,
                                       this.contextPath,
                                       threadFactory,
                                       nettyClientSslBuilder,
                                       mediaTypeCodecRegistry,
                                       AnnotationMetadataResolver.DEFAULT)
        log.debug('Client created to connect to {}', hostUrl)
    }

    Map<String, Object> getDoiProfileEntry(String entryId) {
        retrieveMapFromClient('/dois/{entryId}', [entryId: entryId])
    }

    URI getHostUri() {
        UriBuilder.of(hostUrl).build()
    }

    Map<String, Object> sendMapToClient(String url, Map body, String username, String password) {
        try {
            HttpRequest request = HttpRequest.POST(UriBuilder.of(url).build(), body).basicAuth(username, password)
            Flowable<Map> response = client.retrieve(request,
                                                     Argument.of(Map, String, Object)) as Flowable<Map>
            response.blockingFirst()
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

    Map<String, Object> putMapToClient(String url, Map body, String username, String password) {
        try {
            HttpRequest request = HttpRequest.PUT(UriBuilder.of(url).build(), body).basicAuth(username, password)
            Flowable<Map> response = client.retrieve(request,
                                                     Argument.of(Map, String, Object)) as Flowable<Map>
            response.blockingFirst()
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

    private Map<String, Object> retrieveMapFromClient(String url, String username, String password) {
        try {
            HttpRequest request = HttpRequest.GET(UriBuilder.of(url).build()).basicAuth(username, password)
            Flowable<Map> response = client.retrieve(request,
                                                     Argument.of(Map, String, Object)) as Flowable<Map>
            response.blockingFirst()
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

    private static Map extractExceptionBody(HttpClientResponseException responseException) {
        try {
            responseException.response.body() as Map
        } catch (Exception ignored) {
            [:]
        }
    }
}
