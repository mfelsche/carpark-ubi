package com.example.carparkubi;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface ChargePointRepository extends PagingAndSortingRepository<ChargePoint, Long> {
    public List<ChargePoint> findByStatus(ChargePoint.Status status);
}
