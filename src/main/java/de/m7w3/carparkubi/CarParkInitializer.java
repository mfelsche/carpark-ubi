package de.m7w3.carparkubi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@Component
public class CarParkInitializer implements CommandLineRunner {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    ChargePointRepository repo;

    @Override
    public void run(String... args) throws Exception {
        initializeDb();
    }

    @Transactional
    public void initializeDb() throws Exception {
        logger.info("initialize db...");
        repo.deleteAll();
        for (long i = 0; i < 10; i++) {
            ChargePoint point = new ChargePoint(i, ChargePoint.Status.AVAILABLE, ChargePoint.ChargeType.OFF, 0L);
            ChargePoint saved = repo.save(point);
            logger.info("created {}", saved.toString());
        }
    }
}
