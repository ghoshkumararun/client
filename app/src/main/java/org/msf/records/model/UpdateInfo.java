package org.msf.records.model;

import com.google.gson.annotations.SerializedName;

import org.msf.records.utils.LexicographicVersion;

/**
 * A Gson object that represents an available update.
 */
public class UpdateInfo {

    @SerializedName("url")
    public String url;

    @SerializedName("version")
    public String version;

    /**
     * Returns the parsed {@link LexicographicVersion} or {@code null} if the version is
     * malformed.
     */
    public LexicographicVersion getParsedVersion() {
        try {
            return version == null ? null : new LexicographicVersion(version);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
