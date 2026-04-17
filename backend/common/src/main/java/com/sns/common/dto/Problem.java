package com.sns.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** RFC 7807 Problem Details payload. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Problem(String type, String title, int status, String detail, String instance) {

    public static Problem of(int status, String title, String detail) {
        return new Problem("/problems/" + slug(title), title, status, detail, null);
    }

    private static String slug(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
