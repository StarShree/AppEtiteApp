package com.example.model;

import java.io.Serializable;

/**
 * Model representing App Version Configuration fetched from Cloud SQL or Local Data Store.
 * Used to enforce minimum app version compliance and deliver update announcements.
 */
public class AppVersionConfig implements Serializable {
    private int id;
    private String platform;
    private int minVersionCode;
    private String minVersionName;
    private int latestVersionCode;
    private String latestVersionName;
    private boolean isForceUpdate;
    private String updateTitle;
    private String updateMessage;
    private String updateUrl;

    public AppVersionConfig() {
        this.platform = "android";
        this.minVersionCode = 1;
        this.minVersionName = "1.0";
        this.latestVersionCode = 1;
        this.latestVersionName = "1.0";
        this.isForceUpdate = false;
        this.updateTitle = "App Update Required";
        this.updateMessage = "A new version of AppEtite is available with essential menu availability and ordering updates. Please update to continue.";
        this.updateUrl = "https://github.com/StarShree/AppEtiteApp/releases";
    }

    public AppVersionConfig(int id, String platform, int minVersionCode, String minVersionName,
                            int latestVersionCode, String latestVersionName, boolean isForceUpdate,
                            String updateTitle, String updateMessage, String updateUrl) {
        this.id = id;
        this.platform = platform != null ? platform : "android";
        this.minVersionCode = minVersionCode;
        this.minVersionName = minVersionName != null ? minVersionName : "1.0";
        this.latestVersionCode = latestVersionCode;
        this.latestVersionName = latestVersionName != null ? latestVersionName : "1.0";
        this.isForceUpdate = isForceUpdate;
        this.updateTitle = updateTitle != null ? updateTitle : "App Update Required";
        this.updateMessage = updateMessage != null ? updateMessage : "Please update to the latest version to continue.";
        this.updateUrl = updateUrl;
    }

    /**
     * Checks whether an update MUST be performed before the user can use the app.
     * True if the running app version is below the minimum required version,
     * or if force update is explicitly enabled and the app is below the latest version.
     */
    public boolean isUpdateRequired(int currentVersionCode) {
        if (currentVersionCode < minVersionCode) {
            return true;
        }
        return isForceUpdate && currentVersionCode < latestVersionCode;
    }

    /**
     * Checks whether an optional / recommended update is available.
     */
    public boolean isUpdateRecommended(int currentVersionCode) {
        return currentVersionCode < latestVersionCode;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public int getMinVersionCode() {
        return minVersionCode;
    }

    public void setMinVersionCode(int minVersionCode) {
        this.minVersionCode = minVersionCode;
    }

    public String getMinVersionName() {
        return minVersionName;
    }

    public void setMinVersionName(String minVersionName) {
        this.minVersionName = minVersionName;
    }

    public int getLatestVersionCode() {
        return latestVersionCode;
    }

    public void setLatestVersionCode(int latestVersionCode) {
        this.latestVersionCode = latestVersionCode;
    }

    public String getLatestVersionName() {
        return latestVersionName;
    }

    public void setLatestVersionName(String latestVersionName) {
        this.latestVersionName = latestVersionName;
    }

    public boolean isForceUpdate() {
        return isForceUpdate;
    }

    public void setForceUpdate(boolean forceUpdate) {
        isForceUpdate = forceUpdate;
    }

    public String getUpdateTitle() {
        return updateTitle;
    }

    public void setUpdateTitle(String updateTitle) {
        this.updateTitle = updateTitle;
    }

    public String getUpdateMessage() {
        return updateMessage;
    }

    public void setUpdateMessage(String updateMessage) {
        this.updateMessage = updateMessage;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

    public void setUpdateUrl(String updateUrl) {
        this.updateUrl = updateUrl;
    }
}
