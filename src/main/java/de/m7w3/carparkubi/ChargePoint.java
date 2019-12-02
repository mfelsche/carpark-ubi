package de.m7w3.carparkubi;

import javax.persistence.*;

@Entity
public class ChargePoint {

    // do not use generated value here
    // we initialize the db with static values from 0-9
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private ChargePoint.Status status;

    @Enumerated(EnumType.STRING)
    private ChargePoint.ChargeType chargeType;

    /**
     * time of the last event
     */
    private long cTime;

    public ChargePoint() {}
    public ChargePoint(Long id, Status status, ChargeType chargeType, long cTime) {
        setId(id);
        setStatus(status);
        setChargeType(chargeType);
        setCTime(cTime);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ChargeType getChargeType() {
        return chargeType;
    }

    public void setChargeType(ChargeType chargeType) {
        this.chargeType = chargeType;
    }

    public long getCTime() {
        return cTime;
    }

    public void setCTime(long cTime) {
        this.cTime = cTime;
    }

    public boolean isAvailable() {
        if (getStatus() == Status.AVAILABLE) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ChargePoint{" +
                "id=" + id +
                ", status=" + status +
                ", chargeType=" + chargeType +
                ", cTime=" + cTime +
                '}';
    }

    public static enum Status {
        AVAILABLE, CHARGING;
    }

    public static enum ChargeType {
        FASTCHARGE(20), SLOWCHARGE(10), OFF(0);

        private final int ampere;

        ChargeType(int ampere) {
            this.ampere = ampere;
        }

        public int getAmpere() {
            return ampere;
        }
    }
}
