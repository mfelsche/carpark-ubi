package com.example.carparkubi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CarParkInitializer implements CommandLineRunner {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    ChargePointRepository repo;

    @Override
    public void run(String... args) throws Exception {
        logger.info("initialiyze db...");
        for (long i = 0; i < 10; i++) {
           ChargePoint point = new ChargePoint();
           point.setStatus(ChargePoint.Status.AVAILABLE);
           point.setChargeType(ChargePoint.ChargeType.OFF);
           point.setCTime(0L);
           ChargePoint saved = repo.save(point);
           logger.info("created {}", saved.toString());
       }
    }
}
