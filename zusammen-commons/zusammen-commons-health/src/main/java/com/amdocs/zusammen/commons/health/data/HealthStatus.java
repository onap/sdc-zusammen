package com.amdocs.zusammen.commons.health.data;


public enum HealthStatus {
    UP("UP"),
    DOWN("DOWN");

    private String name;

    HealthStatus(String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        return name;
    }

    public static final HealthStatus toValue(String inVal){
        for (HealthStatus val : values()){
            if (val.toString().equals(inVal)){
                return val;
            }
        }
        return null;
    }
}
