package com.amdocs.zusammen.commons.health.data;


public class HealthInfo  {
    protected String moduleName;
    protected HealthStatus healthStatus;
    protected String description;

    public HealthInfo() {
    }

    public HealthInfo(String moduleName, HealthStatus healthStatus, String description) {
        this.moduleName = moduleName;
        this.healthStatus = healthStatus;
        this.description = description;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "BasicHealthInfo{" +
                "moduleName='" + moduleName + '\'' +
                ", healthStatus=" + healthStatus +
                ", description='" + description + '\'' +
                '}';
    }
}
