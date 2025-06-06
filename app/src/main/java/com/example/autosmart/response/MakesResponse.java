package com.example.autosmart.response;

import java.util.List;

// MakesResponse.java
public class MakesResponse {
    public List<Make> Makes;

    public static class Make {
        public String make_id;
        public String make_display;
    }
}
