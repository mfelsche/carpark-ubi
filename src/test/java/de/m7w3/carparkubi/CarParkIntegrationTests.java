package de.m7w3.carparkubi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CarParkIntegrationTests {

    @Autowired
    CarParkInitializer initializer;

    @Autowired
    ChargePointRepository repo;

    @BeforeEach
    void beforeEach() throws Exception {
        initializer.initializeDb();
    }

    @Test
    void plugValid(@Autowired MockMvc mvc) throws Exception {
        mvc.perform(post("/chargepoint/1/event?event=plug&timestamp=1000"))
                .andExpect(status().isOk());
    }

    @Test
    void plugUnknownChargepointTest(@Autowired MockMvc mvc) throws Exception {
        mvc.perform(post("/chargepoint/11/event?event=plug&timestamp=1234567890"))
                .andExpect(status().isNotFound());
    }

    @Test
    void plugUnknownEvent(@Autowired MockMvc mvc) throws Exception {
        mvc.perform(post("/chargepoint/9/event?event=foo&timestamp=12345"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void plugMissingEvent(@Autowired MockMvc mvc) throws Exception {
        mvc.perform(post("/chargepoint/9/event?timestamp=999999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void plugMissingTimestamp(@Autowired MockMvc mvc) throws Exception {
        mvc.perform(post("/chargepoint/9/event?event=plug"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void plugCheckState(@Autowired MockMvc mvc) throws Exception {
        mvc.perform(post("/chargepoint/1/event?event=plug&timestamp=1000"))
                .andExpect(status().isOk());
        List<ChargePoint> charging =
                repo.findByStatus(ChargePoint.Status.CHARGING);
        assertThat(
                charging,
                hasSize(1)
        );
        assertThat(charging.get(0).getId(), equalTo(1L));
        assertThat(charging.get(0).getChargeType(), equalTo(ChargePoint.ChargeType.FASTCHARGE));
        assertThat(charging.get(0).getCTime(), equalTo(1000L));
    }

    @Test
    void plugUnplug(@Autowired MockMvc mvc) throws Exception {
        List<ChargePoint> before = repo.findByStatus(ChargePoint.Status.CHARGING);
        assertThat(before, hasSize(0));
        mvc.perform(post("/chargepoint/1/event?event=plug&timestamp=1000"))
                .andExpect(status().isOk());
        assertThat(repo.findByStatus(ChargePoint.Status.CHARGING), hasSize(1));
        mvc.perform(post("/chargepoint/1/event?event=unplug&timestamp=1001"))
                .andExpect(status().isOk());
        assertThat(repo.findByStatus(ChargePoint.Status.CHARGING), hasSize(0));
    }

    @Test
    void unplugIgnoreMultiple(@Autowired MockMvc mvc) throws Exception {
        List<ChargePoint> before = repo.findByStatus(ChargePoint.Status.CHARGING);
        mvc.perform(post("/chargepoint/1/event?event=unplug&timestamp=1001"))
                .andExpect(status().isOk());
        assertThat(
                repo.findByStatus(ChargePoint.Status.CHARGING),
                equalTo(before)
        );
        mvc.perform(post("/chargepoint/1/event?event=unplug&timestamp=1001"))
                .andExpect(status().isOk());
        assertThat(
                repo.findByStatus(ChargePoint.Status.CHARGING),
                equalTo(before)
        );
    }

    private long getUsedAmpere() {
        long ampere = 0;
        for (ChargePoint cp : repo.findByStatus(ChargePoint.Status.CHARGING)) {
            ampere += cp.getChargeType().getAmpere();
        }
        return ampere;
    }

    private void plug(long id, MockMvc mvc, long timestamp) throws Exception {
        mvc.perform(post(String.format("/chargepoint/%s/event?event=plug&timestamp=%s", id, timestamp)))
                .andExpect(status().isOk());
    }

    private void unplug(long id, MockMvc mvc, long timestamp) throws Exception {
        mvc.perform(post(String.format("/chargepoint/%s/event?event=unplug&timestamp=%s", id, timestamp)))
                .andExpect(status().isOk());
    }
    @Test
    void plugUnplugAll(@Autowired MockMvc mvc) throws Exception {
        assertThat(getUsedAmpere(), equalTo(0L));
        plug(0L, mvc, 1L);
        assertThat(getUsedAmpere(), equalTo(20L));
        plug(1L, mvc, 2L);
        assertThat(getUsedAmpere(), equalTo(40L));
        plug(2L, mvc, 3L);
        assertThat(getUsedAmpere(), equalTo(60L));
        plug(3L, mvc, 4L);
        assertThat(getUsedAmpere(), equalTo(80L));
        plug(4L, mvc, 5L);
        assertThat(getUsedAmpere(), equalTo((long) ChargePointService.MAX_AMPERE));
        plug(5L, mvc, 6L);
        assertThat(getUsedAmpere(), equalTo((long) ChargePointService.MAX_AMPERE));
        plug(6L, mvc, 7L);
        assertThat(getUsedAmpere(), equalTo((long) ChargePointService.MAX_AMPERE));
        plug(7L, mvc, 8L);
        assertThat(getUsedAmpere(), equalTo((long) ChargePointService.MAX_AMPERE));
        plug(8L, mvc, 9L);
        assertThat(getUsedAmpere(), equalTo((long) ChargePointService.MAX_AMPERE));
        plug(9L, mvc, 10L);
        assertThat(getUsedAmpere(), equalTo((long) ChargePointService.MAX_AMPERE));

        unplug(9L, mvc, 11L);
        assertThat(getUsedAmpere(), equalTo((long) ChargePointService.MAX_AMPERE));
        unplug(8L, mvc, 12L);
        assertThat(getUsedAmpere(), equalTo((long) ChargePointService.MAX_AMPERE));
        unplug(7L, mvc, 13L);
        assertThat(getUsedAmpere(), equalTo((long) ChargePointService.MAX_AMPERE));
        unplug(6L, mvc, 14L);
        assertThat(getUsedAmpere(), equalTo((long) ChargePointService.MAX_AMPERE));
        unplug(5L, mvc, 15L);
        assertThat(getUsedAmpere(), equalTo((long) ChargePointService.MAX_AMPERE));
        unplug(4L, mvc, 16L);
        assertThat(getUsedAmpere(), equalTo(80L));
        unplug(3L, mvc, 17L);
        assertThat(getUsedAmpere(), equalTo(60L));
        unplug(2L, mvc, 18L);
        assertThat(getUsedAmpere(), equalTo(40L));
        unplug(1L, mvc, 19L);
        assertThat(getUsedAmpere(), equalTo(20L));
        unplug(0L, mvc, 20L);
        assertThat(getUsedAmpere(), equalTo(0L));

    }
}