package com.socketbytes.hexiscale.util;

import android.bluetooth.BluetoothDevice;

/**
 * Created by emmanuel on 9/27/2016.
 */
public class ApplicationSession {

    private String username;
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpires;
    private long refreshTokenExpires;
    private BluetoothDevice bluetoothDevice;

    private static ApplicationSession ourInstance = new ApplicationSession();

    public static ApplicationSession getInstance() {
        return ourInstance;
    }

    private ApplicationSession() {
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getAccessTokenExpires() {
        return accessTokenExpires;
    }

    public void setAccessTokenExpires(long accessTokenExpires) {
        this.accessTokenExpires = accessTokenExpires;
    }

    public long getRefreshTokenExpires() {
        return refreshTokenExpires;
    }

    public void setRefreshTokenExpires(long refreshTokenExpires) {
        this.refreshTokenExpires = refreshTokenExpires;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }
}
