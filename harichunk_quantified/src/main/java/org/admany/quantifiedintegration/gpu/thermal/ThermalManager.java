package org.admany.quantifiedintegration.gpu.thermal;

public final class ThermalManager {

    private static final double DEFAULT_LIMIT_C = Double.parseDouble(
            System.getProperty("harichunk.gpu_noise.thermal_limit", "90.0")
    );
    private static final double RECOVERY_HYSTERESIS_C = Double.parseDouble(
            System.getProperty("harichunk.gpu_noise.thermal_recovery_hysteresis", "5.0")
    );

    private volatile boolean thermallyLimited;
    private volatile double lastTemperatureC = Double.NaN;

    public boolean isThermallyLimited() {
        return thermallyLimited;
    }

    public double getLastTemperatureC() {
        return lastTemperatureC;
    }

    public void updateThermalLimiter(double temperatureC) {
        lastTemperatureC = temperatureC;
        if (temperatureC >= DEFAULT_LIMIT_C) {
            thermallyLimited = true;
        } else if (temperatureC <= DEFAULT_LIMIT_C - RECOVERY_HYSTERESIS_C) {
            thermallyLimited = false;
        }
    }
}
