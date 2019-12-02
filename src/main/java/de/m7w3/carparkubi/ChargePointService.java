package de.m7w3.carparkubi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Iterator;

@Service
public class ChargePointService {

    public static final int MAX_AMPERE = 100;

    private ChargePointRepository repo;

    public ChargePointService(@Autowired ChargePointRepository repo) {
        this.repo = repo;
    }

    /**
     * Made threadsafe by encapsulating it into a JPA transaction.
     * TODO: retry on transaction error
     * @param cp ChargePoint to start charging
     * @param timestamp timestamp of the plug-in event
     */
    @Transactional
    public void startCharging(ChargePoint cp, Long timestamp) {
        int consumedAmpere = 0;
        for (ChargePoint chargingCp : repo.findByStatus(ChargePoint.Status.CHARGING)) {
            consumedAmpere += chargingCp.getChargeType().getAmpere();
        }
        int remainingAmpere = MAX_AMPERE - consumedAmpere;

        ChargePoint.ChargeType chargeType = ChargePoint.ChargeType.SLOWCHARGE;
        if (remainingAmpere >= ChargePoint.ChargeType.FASTCHARGE.getAmpere()) {
            chargeType = ChargePoint.ChargeType.FASTCHARGE;
        } else {
            // lets switch the oldest charging point to slowcharge if available
            for (ChargePoint sortedCp: repo.findAll(Sort.by(Sort.Order.asc("cTime")))) {
                if (sortedCp.getId().equals(cp.getId())) {
                    continue;
                }
                remainingAmpere += switchToSlowCharge(sortedCp);
                if (remainingAmpere >= ChargePoint.ChargeType.FASTCHARGE.getAmpere()) {
                    // if we were able to switch enough
                    // chargepoints to slow charge
                    chargeType = ChargePoint.ChargeType.FASTCHARGE;
                    break;
                }
            }
        }
        cp.setChargeType(chargeType);
        cp.setStatus(ChargePoint.Status.CHARGING);
        cp.setCTime(timestamp);
        repo.save(cp);
    }

    /**
     *
     * @param cp ChargePoint to switch if on FASTCHARGE
     * @return freed ampere that are now available again.
     */
    private int switchToSlowCharge(ChargePoint cp) {
        ChargePoint.ChargeType oldType = cp.getChargeType();
        if (oldType == ChargePoint.ChargeType.FASTCHARGE) {
            cp.setChargeType(ChargePoint.ChargeType.SLOWCHARGE);
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
        ChargePoint.ChargeType oldType = cp.getChargeType();
        if (oldType == ChargePoint.ChargeType.SLOWCHARGE) {
            cp.setChargeType(ChargePoint.ChargeType.FASTCHARGE);
            repo.save(cp);
        }
        return cp.getChargeType().getAmpere() - oldType.getAmpere();
    }

    @Transactional
    public void unplug(ChargePoint cp, Long timestamp) {
        ChargePoint.ChargeType oldType = cp.getChargeType();
        int remainingAmpere = oldType.getAmpere();
        // switch to fastcharge if possible, prioritize cars plugged in more recently
        Iterator<ChargePoint> cpsToSwitch = repo.findAll(Sort.by(Sort.Order.desc("cTime"))).iterator();
        while (remainingAmpere >= ChargePoint.ChargeType.SLOWCHARGE.getAmpere() && cpsToSwitch.hasNext()) {
            ChargePoint switchMe = cpsToSwitch.next();
            if (switchMe.getId().equals(cp.getId())) { continue; }
            remainingAmpere -= switchToFastCharge(switchMe);
        }

        cp.setChargeType(ChargePoint.ChargeType.OFF);
        cp.setStatus(ChargePoint.Status.AVAILABLE);
        cp.setCTime(timestamp);
        repo.save(cp);
    }


}
