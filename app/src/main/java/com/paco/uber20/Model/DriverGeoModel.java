package com.paco.uber20.Model;

import com.firebase.geofire.GeoLocation;

public class DriverGeoModel {
    private String key;
    private GeoLocation geoLocation;
    private driverModel driverInfoModel;

    public DriverGeoModel() {
    }

    public DriverGeoModel(String key, GeoLocation geoLocation) {
        this.key = key;
        this.geoLocation = geoLocation;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(GeoLocation geoLocation) {
        this.geoLocation = geoLocation;
    }

    public driverModel getDriverInfoModel() {
        return driverInfoModel;
    }

    public void setDriverInfoModel(driverModel driverInfoModel) {
        this.driverInfoModel = driverInfoModel;
    }
}
