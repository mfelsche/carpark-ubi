package de.m7w3.carparkubi;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface ChargePointRepository extends PagingAndSortingRepository<ChargePoint, Long> {
    public List<ChargePoint> findByStatus(ChargePoint.Status status);
}
