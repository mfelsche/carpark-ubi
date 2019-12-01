package com.example.carparkubi;

import com.example.carparkubi.ChargePoint.ChargeType;
import com.example.carparkubi.ChargePoint.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Iterator;
import java.util.List;

@Service
public class CarParkService {

    private static final int MAX_AMPERE = 100;

    @Autowired
    ChargePointRepository repo;

    /**
     * Made threadsafe by encapsulating it into a JPA transaction.
     * TODO: retry on transaction error
     * @param cp ChargePoint to start charging
     * @param timestamp timestamp of the plug-in event
     */
    @Transactional
    public void startCharging(ChargePoint cp, Long timestamp) {
        int consumedAmpere = 0;
        for (ChargePoint chargingCp : repo.findByStatus(Status.CHARGING)) {
            consumedAmpere += chargingCp.getChargeType().getAmpere();
        }
        int remainingAmpere = MAX_AMPERE - consumedAmpere;

        ChargeType chargeType = ChargeType.SLOWCHARGE;
        if (remainingAmpere >= ChargeType.FASTCHARGE.getAmpere()) {
            chargeType = ChargeType.FASTCHARGE;
        } else {
            // lets switch the oldest charging point to slowcharge if available
            for (ChargePoint sortedCp: repo.findAll(Sort.by(Sort.Order.asc("cTime")))) {
                if (sortedCp.getId().equals(cp.getId())) {
                    continue;
                }
                remainingAmpere += switchToSlowCharge(sortedCp);
                if (remainingAmpere >= ChargeType.FASTCHARGE.getAmpere()) {
                    // if we were able to switch enough
                    // chargepoints to slow charge
                    chargeType = ChargeType.FASTCHARGE;
                    break;
                }
            }
        }
        cp.setChargeType(chargeType);
        cp.setStatus(Status.CHARGING);
        cp.setCTime(timestamp);
        repo.save(cp);
    }

    /**
     *
     * @param cp ChargePoint to switch if on FASTCHARGE
     * @return freed ampere that are now available again.
     */
    private int switchToSlowCharge(ChargePoint cp) {
        ChargeType oldType = cp.getChargeType();
        if (oldType == ChargeType.FASTCHARGE) {
            cp.setChargeType(ChargeType.SLOWCHARGE);
            repo.save(cp);
        }
        return Math.abs(oldType.getAmpere() - cp.getChargeType().getAmpere());
    }

    /**
     *
     * @param cp ChargePoint to switch if on SLOWCHARGE
     * @return consumed ampere.
     */
    private int switchToFastCharge(ChargePoint cp) {
        ChargeType oldType = cp.getChargeType();
        if (oldType == ChargeType.SLOWCHARGE) {
            cp.setChargeType(ChargeType.FASTCHARGE);
            repo.save(cp);
        }
        return cp.getChargeType().getAmpere() - oldType.getAmpere();
    }

    @Transactional
    public void unplug(ChargePoint cp, Long timestamp) {
        ChargeType oldType = cp.getChargeType();
        int remainingAmpere = oldType.getAmpere();
        // switch to fastcharge if possible, prioritize cars plugged in more recently
        Iterator<ChargePoint> cpsToSwitch = repo.findAll(Sort.by(Sort.Order.desc("cTime"))).iterator();
        while (remainingAmpere >= ChargeType.SLOWCHARGE.getAmpere() && cpsToSwitch.hasNext()) {
            remainingAmpere -= switchToFastCharge(cpsToSwitch.next());
        }
        assert remainingAmpere == 0;

        cp.setChargeType(ChargeType.OFF);
        cp.setStatus(Status.AVAILABLE);
        cp.setCTime(timestamp);
        repo.save(cp);
    }


}
