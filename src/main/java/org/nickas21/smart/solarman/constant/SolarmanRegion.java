package org.nickas21.smart.solarman.constant;

public enum SolarmanRegion {

    /**
     * China
     */
    CN("https://api.solarmanpv.com", "pulsar+ssl://mqe.tuyacn.com:7285/"),
    /**
     * International
     */
    IN("https://globalapi.solarmanpv.com", "pulsar+ssl://mqe.tuyain.com:7285/");

    private final String apiUrl;

    private final String msgUrl;

    SolarmanRegion(String apiUrl, String msgUrl) {
        this.apiUrl = apiUrl;
        this.msgUrl = msgUrl;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getMsgUrl() {
        return msgUrl;
    }
}

