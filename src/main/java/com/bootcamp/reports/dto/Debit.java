package com.bootcamp.reports.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * Clase de transferencia de datos para el microservicio de tarjetas de debito
 */
@Data
@JsonInclude(Include.NON_NULL)
public class Debit {
	private String id;
	private String customerId;
	private String cardNumber;
	private Double amount;
	private String customerType;
	private LocalDateTime debitDate;
	private List<String> accountId;
	private String productType;
}
