package org.nickas21.smart.solarman.constant;

public enum SolarmanRegion {

    /**
     * China
     */
    CN("https://api.solarmanpv.com"),
    /**
     * International
     */
    IN("https://globalapi.solarmanpv.com");

    private final String apiUrl;

    SolarmanRegion(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiUrl() {
        return apiUrl;
    }
}

