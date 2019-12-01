package com.example.carparkubi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class CarParkController {

    public static class ChargePointNotFoundException extends RuntimeException {
        private final Long id;

        public ChargePointNotFoundException(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }
    }

    public static class ErrorResponse {
        private final String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    ChargePointRepository repo;

    @Autowired
    CarParkService carParkService;

    @ExceptionHandler(ChargePointNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorResponse chargePointNotFoundHandler(ChargePointNotFoundException ex) {
        return new ErrorResponse("charge-point " + ex.getId() + " not found.");
    }

    @PostMapping(path = "/chargepoint/{chargePointId}/event", params = "event=plug")
    @ResponseStatus(HttpStatus.OK)
    public void plug(@PathVariable Long chargePointId, @RequestParam(name="timestamp", required = true) Long timestamp, HttpServletResponse response) {

        Optional<ChargePoint> cpo = repo.findById(chargePointId);
        if (cpo.isPresent()) {
            ChargePoint cp = cpo.get();
            if (!cp.isAvailable()) {
                // let's assume we didn't unplug before, due to missed unplug event
                // or events coming in out of order, so unplug now, to ensure valid state
                carParkService.unplug(cp, timestamp);
            }
            // try to get fast-charge, if possible switch old chargepoints to slowcharge to achieve it
            // change status of ChargePoint
            carParkService.startCharging(cp, timestamp);
        } else {
            throw new ChargePointNotFoundException(chargePointId);
        }
    }

    @PostMapping(path = "/chargepoint/{chargePointId}/event", params = "event=unplug")
    @ResponseStatus(HttpStatus.OK)
    public void unplug(@PathVariable Long chargePointId, @RequestParam(name="timestamp", required = true) Long timestamp) {
        // try to allocate available ampere to currently charging cars sorted from newest to oldest
        // switch 1 car to fastcharge if chargePoint was on slowcharge
        // switch 2 cars to fastcharge if chargePoint was on fastcharge
        // do nothing if chargePoint has not been plugged
        Optional<ChargePoint> cpo = repo.findById(chargePointId);
        if (cpo.isPresent()) {
            ChargePoint cp = cpo.get();
            if (cp.isAvailable()) {
                // do nothing, we might receive this out of order
                logger.info("Chargepoint unplugged while being unplugged...");
            } else {
                carParkService.unplug(cp, timestamp);
            }
        }
    }

    public static class ChargePointReport {
        private final Long chargePointId;
        private final String status;
        private final int consumption;
        private final OffsetDateTime since;

        public ChargePointReport(Long chargePointId, String status, int consumption, OffsetDateTime since) {
            this.chargePointId = chargePointId;
            this.status = status;
            this.consumption = consumption;
            this.since = since;
        }

        public int getConsumption() {
            return consumption;
        }

        public String getStatus() {
            return status;
        }

        public Long getChargePointId() {
            return chargePointId;
        }

        public static ChargePointReport fromChargePoint(ChargePoint cp) {
            return new ChargePointReport(
                    cp.getId(),
                    cp.getStatus().name(),
                    cp.getChargeType().getAmpere(),
                    OffsetDateTime.ofInstant(Instant.ofEpochSecond(cp.getCTime()), ZoneOffset.UTC));
        }

        public OffsetDateTime getSince() {
            return since;
        }
    }

    @GetMapping("/report")
    @ResponseBody
    public List<ChargePointReport> report() {
        List<ChargePointReport> reports = new ArrayList<>(10);
        for (ChargePoint cp : repo.findAll(Sort.by(Sort.Order.asc("id")))) {
            reports.add(ChargePointReport.fromChargePoint(cp));
        }
        return reports;
    }

}
