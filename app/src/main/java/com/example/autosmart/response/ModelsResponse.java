package com.example.autosmart.response;

import java.util.List;

// ModelsResponse.java
public class ModelsResponse {
    public List<Model> Models;

    public static class Model {
        public String model_name;
        public String model_make_id;
    }
}