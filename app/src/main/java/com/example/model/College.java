package com.example.model;

import java.io.Serializable;

public class College implements Serializable {
    public static final String CODE_STANFORD = "STAN";
    public static final String CODE_IMPERIAL = "IMP";
    public static final String CODE_ST_JUDE = "SIT";

    private int collegeId;
    private String idString; // Short ID: "STAN", "IMP", "SIT"
    private String name;
    private String location;

    public College() {}

    public College(int collegeId, String name, String location) {
        this.collegeId = collegeId;
        this.name = name;
        this.location = location;
        this.idString = toShortCode(null, collegeId, name);
    }

    public College(String idString, String name, String location) {
        this.name = name;
        this.location = location;
        this.idString = toShortCode(idString, null, name);
        this.collegeId = codeToNumericId(this.idString);
    }

    public College(String idString, int collegeId, String name, String location) {
        this.collegeId = collegeId;
        this.name = name;
        this.location = location;
        this.idString = toShortCode(idString, collegeId, name);
    }

    /**
     * Converts any raw college identifier (e.g. "col_1", "1", "STAN", "Stanford...")
     * into the canonical concise uppercase short ID: "STAN", "IMP", "SIT".
     */
    public static String toShortCode(String rawId, Integer numericId, String name) {
        if (rawId != null) {
            String u = rawId.trim().toUpperCase();
            if (u.equals(CODE_STANFORD) || u.equals("COL_1") || u.equals("1") || u.contains("STANFORD")) {
                return CODE_STANFORD;
            }
            if (u.equals(CODE_IMPERIAL) || u.equals("ICE") || u.equals("COL_2") || u.equals("2") || u.contains("IMPERIAL")) {
                return CODE_IMPERIAL;
            }
            if (u.equals(CODE_ST_JUDE) || u.equals("SJIT") || u.equals("COL_3") || u.equals("3") || u.contains("JUDE") || u.contains("STJUDE")) {
                return CODE_ST_JUDE;
            }
        }
        if (numericId != null) {
            if (numericId == 1) return CODE_STANFORD;
            if (numericId == 2) return CODE_IMPERIAL;
            if (numericId == 3) return CODE_ST_JUDE;
        }
        if (name != null) {
            String u = name.toUpperCase();
            if (u.contains("STANFORD")) return CODE_STANFORD;
            if (u.contains("IMPERIAL")) return CODE_IMPERIAL;
            if (u.contains("JUDE")) return CODE_ST_JUDE;
        }
        if (rawId != null && !rawId.trim().isEmpty()) {
            return rawId.trim().toUpperCase();
        }
        return CODE_STANFORD;
    }

    /**
     * Converts a short code to the numeric ID (1, 2, 3).
     */
    public static int codeToNumericId(String code) {
        if (code == null) return 1;
        String u = code.trim().toUpperCase();
        if (u.equals(CODE_STANFORD) || u.equals("COL_1") || u.equals("1") || u.contains("STANFORD")) return 1;
        if (u.equals(CODE_IMPERIAL) || u.equals("ICE") || u.equals("COL_2") || u.equals("2") || u.contains("IMPERIAL")) return 2;
        if (u.equals(CODE_ST_JUDE) || u.equals("SJIT") || u.equals("COL_3") || u.equals("3") || u.contains("JUDE") || u.contains("STJUDE")) return 3;
        try {
            if (u.startsWith("COL_")) return Integer.parseInt(u.substring(4));
            return Integer.parseInt(u);
        } catch (Exception e) {
            return 1;
        }
    }

    public static String getCollegeDisplayName(String code) {
        String shortCode = toShortCode(code, null, null);
        if (CODE_STANFORD.equals(shortCode)) return "Stanford University Campus";
        if (CODE_IMPERIAL.equals(shortCode)) return "Imperial College of Engineering";
        if (CODE_ST_JUDE.equals(shortCode)) return "St. Jude Institute of Technology";
        return shortCode;
    }

    public String getIdString() {
        return toShortCode(idString, collegeId, name);
    }

    public void setIdString(String idString) {
        this.idString = toShortCode(idString, collegeId, name);
    }

    public int getCollegeId() {
        if (collegeId <= 0) {
            collegeId = codeToNumericId(idString);
        }
        return collegeId;
    }

    public void setCollegeId(int collegeId) {
        this.collegeId = collegeId;
        if (this.idString == null || this.idString.isEmpty()) {
            this.idString = toShortCode(null, collegeId, name);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public String getAddress() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "[" + getIdString() + "] " + name;
    }
}
