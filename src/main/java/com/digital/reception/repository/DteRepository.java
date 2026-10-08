package com.digital.reception.repository;

import com.digital.reception.entity.DteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DteRepository extends JpaRepository<DteEntity, String> {
    Optional<DteEntity> findByCodigoGeneracionAndNitEmisorAndTdte(
            String codigoGeneracion, String nitEmisor, String tdte);
}
