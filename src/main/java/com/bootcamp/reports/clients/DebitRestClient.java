package com.bootcamp.reports.clients;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.bootcamp.reports.clients.config.RestConfig;
import com.bootcamp.reports.dto.Debit;

import reactor.core.publisher.Flux;

/**
 * Clase de acceso al microservicio de clientes
 */
@Service
public class DebitRestClient {
    RestConfig restConfig = new RestConfig();
	
	public Flux<Debit> getAllDebitsXCustomerId(String customerId) {
		WebClient webClient = WebClient.create("http://localhost:8088");
        return  webClient.get()
                .uri("/debit/customer/alldebits/"+customerId)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToFlux(Debit.class);
	}
}
