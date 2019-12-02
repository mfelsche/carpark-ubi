package de.m7w3.carparkubi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest
public class CarParkMultiThreadingTest {

    @Autowired
    ChargePointService service;

    @Autowired
    ChargePointRepository repo;

    @Test
    void multiThreadedAccess() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        for (ChargePoint cp: repo.findAll()) {
            pool.execute(() -> {
                service.startCharging(cp, 1L + cp.getId());
                assertThat(getUsedAmpere(), lessThanOrEqualTo(ChargePointService.MAX_AMPERE));
                service.unplug(cp, 2L + cp.getId());
            });
            pool.execute(() -> {
                service.startCharging(cp, 3L + cp.getId());
                assertThat(getUsedAmpere(), lessThanOrEqualTo(ChargePointService.MAX_AMPERE));
                service.unplug(cp, 2L + cp.getId());
            });
        }

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(
                getUsedAmpere(), equalTo(0)
        );
    }

    private int getUsedAmpere() {
        int ampere = 0;
        for (ChargePoint cp : repo.findByStatus(ChargePoint.Status.CHARGING)) {
            ampere += cp.getChargeType().getAmpere();
        }
        return ampere;
    }
}
