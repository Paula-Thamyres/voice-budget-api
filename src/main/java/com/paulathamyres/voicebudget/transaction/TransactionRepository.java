package com.paulathamyres.voicebudget.transaction;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByOccurredOnBetweenOrderByOccurredOnDescIdDesc(LocalDate start, LocalDate end);

    List<Transaction> findAllByOrderByOccurredOnDescIdDesc(Limit limit);
}
