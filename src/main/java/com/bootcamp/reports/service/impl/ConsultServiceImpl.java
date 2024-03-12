package com.bootcamp.reports.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bootcamp.reports.clients.AccountsRestClient;
import com.bootcamp.reports.clients.CreditsRestClient;
import com.bootcamp.reports.clients.CustomersRestClient;
import com.bootcamp.reports.clients.DebitRestClient;
import com.bootcamp.reports.clients.TransactionsRestClient;
import com.bootcamp.reports.dto.Accumulator;
import com.bootcamp.reports.dto.AverageDay;
import com.bootcamp.reports.dto.AverageMovements;
import com.bootcamp.reports.dto.Customer;
import com.bootcamp.reports.dto.DebitCreditMovements;
import com.bootcamp.reports.dto.Movements;
import com.bootcamp.reports.dto.Products;
import com.bootcamp.reports.dto.Transaction;
import com.bootcamp.reports.service.ConsultService;

import reactor.core.publisher.Mono;

/**
 * Clase de implementación para la interfaz ConsultService
 */
@Service
public class ConsultServiceImpl implements ConsultService{

	@Autowired
	AccountsRestClient accountsRestClient;
	
	@Autowired
	CreditsRestClient creditsRestClient;
	
	@Autowired
	TransactionsRestClient transactionsRestClient;
	
	@Autowired
	CustomersRestClient customersRestClient;
	
	@Autowired
	DebitRestClient debitRestClient;

	/**
	 * Devuelve la lista de productos de un cliente personal segun el id de cliente.
	 * @param customerId
	 * @return Mono<Products>
	 */
	@Override
	public Mono<Products> productXCustomerIdPerson(String customerId) {
        return customersRestClient.getPersonById(customerId).flatMap(p -> {
        	Customer customer = new Customer();
        	customer.setId(p.getId());
			customer.setDocument(p.getDni());
	        customer.setNameCustomer(p.getName().concat(" ").concat(p.getLastName()));
	        customer.setTypeCustomer(p.getTypeCustomer());
        	return obtainProducts(customer, customerId);
        });
	}

	/**
	 * Devuelve la lista de productos de un cliente empresarial segun el id de cliente.
	 * @param customerId
	 * @return Mono<Products>
	 */
	@Override
	public Mono<Products> productXCustomerIdCompany(String customerId) {
		return customersRestClient.getCompanyById(customerId).flatMap(p -> {
			Customer customer = new Customer();
			customer.setId(p.getId());
			customer.setDocument(p.getRuc());
	        customer.setNameCustomer(p.getBusinessName());
	        customer.setTypeCustomer(p.getTypeCustomer());
        	return obtainProducts(customer, customerId);
        });
	}

	/***
	 * Obtiene la lista de productos de los clientes
	 * @param customer
	 * @param customerId
	 * @return
	 */
	private Mono<Products> obtainProducts(Customer customer, String customerId){
        return accountsRestClient.getAllAccountXCustomerId(customerId).collectList().flatMap(acounts -> {
        	return creditsRestClient.getAllCreditXCustomerId(customerId).collectList().flatMap(credits -> {
        		return creditsRestClient.getAllCreditCardXCustomerId(customerId).collectList().flatMap(creditCards -> {
        			return debitRestClient.getAllDebitsXCustomerId(customerId).collectList().flatMap(debits -> {
            			return Mono.just(new Products(customer, acounts, credits, creditCards,debits));
            		});
        		});
        	});
        });
	}

	/**
	 * Muestra la lista de movimientos de una cuenta segun su id.
	 * @param id
	 * @return Mono<Movements>
	 */
	@Override
	public Mono<Movements> movementXAccountId(String id) {
		return transactionsRestClient.getAllXProductId(id).collectList().flatMap(transactions ->{
			return accountsRestClient.getAccountById(id).flatMap(account -> {
				return obtainCustomer(transactions, account.getCustomerId(), account.getTypeCustomer());
			});
		});	
	};

	/**
	 * Muestra la lista de movimientos de un credito segun su id.
	 * @param id
	 * @return Mono<Movements>
	 */
	@Override
	public Mono<Movements> movementXCreditId(String id) {
		return transactionsRestClient.getAllXProductId(id).collectList().flatMap(transactions ->{
			return creditsRestClient.getCreditById(id).flatMap(credit -> {
				return obtainCustomer(transactions, credit.getCustomerId(), credit.getTypeCustomer());
			});
		});	
	};

	/**
	 * Muestra la lista de movimientos de una tarjeta de credito segun su id.
	 * @param id
	 * @return Mono<Movements>
	 */
	@Override
	public Mono<Movements> movementXCreditCardId(String id) {
		return transactionsRestClient.getAllXProductId(id).collectList().flatMap(transactions ->{
			return creditsRestClient.getCreditCardById(id).flatMap(creditCard -> {
				return obtainCustomer(transactions, creditCard.getCustomerId(), creditCard.getTypeCustomer());
			});
		});	
	};

	/**
	 * Obtiene al cliente segun su tipo(empresarial/personal) y lo convierte en una clase Customer
	 * @param listTransaction
	 * @param id
	 * @param type
	 * @return Mono<Movements>
	 */
	private Mono<Movements> obtainCustomer(List<Transaction> listTransaction, String id, String type){
		if(type.equals("PERSON")) {
			return customersRestClient.getPersonById(id).flatMap(p -> {
				Customer customer = new Customer();
				customer.setDocument(p.getDni());
				customer.setNameCustomer(p.getName().concat(" ").concat(p.getLastName()));
				customer.setTypeCustomer(p.getTypeCustomer());
				return Mono.just(new Movements(customer, listTransaction));
			});
		}else {
			return customersRestClient.getCompanyById(id).flatMap(p -> {
				Customer customer = new Customer();
				customer.setDocument(p.getRuc());
				customer.setNameCustomer(p.getBusinessName());
				customer.setTypeCustomer(p.getTypeCustomer());
				return Mono.just(new Movements(customer, listTransaction));
			});
		}
	}
	
	@Override
	public Mono<Movements> commissionXAccountId(String id) {
		LocalDateTime myDateObj = LocalDateTime.now();
		return transactionsRestClient.getAllXProductId(id)
				.filter(a -> a.getTransactionType().equals("COMISION"))
				.filter(a -> (a.getTransactionDate().getMonthValue()==(myDateObj.getMonthValue())) && (a.getTransactionDate().getYear()==(myDateObj.getYear())))
				.collectList().flatMap(transactions ->{
					return accountsRestClient.getAccountById(id).flatMap(a -> {
						return obtainCustomer(transactions, a.getCustomerId(), a.getTypeCustomer());
				});
		});	
	}

    @Override
    public Mono<AverageMovements> averageBalancesXCustomerIdPerson(String id) {
    	return customersRestClient.getPersonById(id).flatMap(p -> {
			Customer customer = new Customer();
			customer.setDocument(p.getDni());
			customer.setNameCustomer(p.getName().concat(" ").concat(p.getLastName()));
			customer.setTypeCustomer(p.getTypeCustomer());
			return obtainTransactionsXCustomerId(id, customer);
		});
    }
    
    @Override
    public Mono<AverageMovements> averageBalancesXCustomerIdCompany(String id) {
    	return customersRestClient.getCompanyById(id).flatMap(p -> {
			Customer customer = new Customer();
			customer.setDocument(p.getRuc());
			customer.setNameCustomer(p.getBusinessName());
			customer.setTypeCustomer(p.getTypeCustomer());
			return obtainTransactionsXCustomerId(id, customer);
		});
    }
    
    private Mono<AverageMovements> obtainTransactionsXCustomerId(String id, Customer customer) {
    	return transactionsRestClient.getAllXCustomerId(id)
				.groupBy(transaction -> transaction.getTransactionDate().toLocalDate())
	            .flatMap(groupedFlux -> {
	                return groupedFlux
	                        .map(Transaction::getBalance)
	                        .reduce(new Accumulator(), (acc, balance) -> acc.add(balance))
	                        .map(accumulator -> {
	                            LocalDate date = groupedFlux.key();
	                            long count = accumulator.getCount();
	                            double averageBalance = accumulator.getTotal() / count;
	                            return new AverageDay(date, averageBalance);
	                        });
	            })
	            .collectList()
	            .map(averageDays -> new AverageMovements(customer, averageDays));
    }

	@Override
	public Mono<DebitCreditMovements> debitsCreditXCustomerId(String id) {
		return transactionsRestClient.getAllXCustomerId(id).filter(d -> d.getProductType().equals("DEBIT_CARD")).take(10).collectList().flatMap(debit -> {
        	return transactionsRestClient.getAllXCustomerId(id).filter(c -> c.getProductType().startsWith("TAR_CRED")).take(10).collectList().flatMap(credit -> {
            		return Mono.just(new DebitCreditMovements(debit, credit));
        	});
        });
	}

}
