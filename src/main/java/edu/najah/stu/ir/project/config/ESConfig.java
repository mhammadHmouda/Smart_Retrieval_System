package edu.najah.stu.ir.project.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.net.ssl.SSLContext;

@Configuration
public class ESConfig {

    @Value("${es.url}")
    private String baseUrl;

    @Value("${es.username}")
    private String username;

    @Value("${es.password}")
    private String password;

    @Bean
    public ElasticsearchClient client() throws Exception {
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true).build();

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        RestClient restClient = RestClient.builder(HttpHost.create(baseUrl))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setSSLContext(sslContext)
                                .setDefaultCredentialsProvider(credentialsProvider))
                .build();

        var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}