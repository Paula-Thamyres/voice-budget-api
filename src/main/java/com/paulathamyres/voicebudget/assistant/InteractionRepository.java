package com.paulathamyres.voicebudget.assistant;

import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    List<Interaction> findAllByOrderByCreatedAtDescIdDesc(Limit limit);
}
