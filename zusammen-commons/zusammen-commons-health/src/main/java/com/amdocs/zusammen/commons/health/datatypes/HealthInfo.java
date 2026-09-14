package com.amdocs.zusammen.commons.health.datatypes;


public class HealthInfo {
    private String healthCheckComponent;
    private HealthStatus healthStatus;
    private String version;
    private String description;

    public HealthInfo() {
    }

    public HealthInfo(String healthCheckComponent, HealthStatus healthStatus, String version, String description) {
        this.healthCheckComponent = healthCheckComponent;
        this.healthStatus = healthStatus;
        this.version = version;
        this.description = description;
    }

    public String getHealthCheckComponent() {
        return healthCheckComponent;
    }

    public void setHealthCheckComponent(String healthCheckComponent) {
        this.healthCheckComponent = healthCheckComponent;
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "HealthInfo{" +
                "healthCheckComponent='" + healthCheckComponent + '\'' +
                ", healthStatus=" + healthStatus +
                ", version='" + version + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
