package de.m7w3.carparkubi;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class CarParkServiceTests {

    @Test
    void startCharging() {
        ChargePointRepository mockedRepo = mock(ChargePointRepository.class);
        List<ChargePoint> allCps = new ArrayList<>(10);
        List<ChargePoint> chargingCps = new ArrayList<>(6);
        for (long i = 0L; i < 5L; i++) {
            ChargePoint cp = new ChargePoint(i, ChargePoint.Status.AVAILABLE, ChargePoint.ChargeType.OFF, i);
            allCps.add(cp);
        }
        for (long j = 5L; j < 10L; j++) {
            ChargePoint chargingCp = new ChargePoint(j, ChargePoint.Status.CHARGING, ChargePoint.ChargeType.FASTCHARGE, j);
            allCps.add(chargingCp);
            chargingCps.add(chargingCp);
        }
        when(mockedRepo.findAll(Mockito.any(Sort.class))).thenReturn(allCps);
        when(mockedRepo.findByStatus(ChargePoint.Status.CHARGING)).thenReturn(chargingCps);
        ChargePointService service = new ChargePointService(mockedRepo);
        service.startCharging(allCps.get(0), 10L);
        // once for the actual chargepoint that starts charging, twice for downgrading existing fastcharge chargepoints
        Mockito.verify(mockedRepo, times(3)).save(Mockito.any(ChargePoint.class));
        assertThat(
                allCps.get(0).getChargeType(),
                equalTo(ChargePoint.ChargeType.FASTCHARGE)
        );
        assertThat(
                allCps.get(0).getStatus(),
                equalTo(ChargePoint.Status.CHARGING)
        );
        assertThat(
                allCps.get(0).getCTime(),
                equalTo(10L)
        );

        int usedAmpere = 0;
        for (ChargePoint cp: allCps) {
            usedAmpere += cp.getChargeType().getAmpere();
        }
        assertThat(usedAmpere, equalTo(ChargePointService.MAX_AMPERE));
    }

}
