package com.paulloweber.goldenraspberry.adapter.out.persistence;

import com.paulloweber.goldenraspberry.adapter.out.persistence.entity.ProducerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProducerJpaRepository extends JpaRepository<ProducerEntity, Long> {
}
