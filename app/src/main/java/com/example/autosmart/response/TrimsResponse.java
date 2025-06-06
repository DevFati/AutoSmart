package com.example.autosmart.response;

import java.util.List;

// TrimsResponse.java
public class TrimsResponse {
    public List<Trim> Trims;

    public static class Trim {
        public String model_year;
        public String make_display;
        public String model_name;
        public String model_trim;
    }
}