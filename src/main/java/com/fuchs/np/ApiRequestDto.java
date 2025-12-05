package com.fuchs.np;

import java.util.Map;

public class ApiRequestDto {
    private String modelName;
    private String calledMethod;
    private Map<String, Object> methodProperties;

    // apiKey ми тут не приймаємо ззовні (або ігноруємо), бо будемо сетати свій
    private String apiKey;

    // Геттери та Сеттери
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getCalledMethod() { return calledMethod; }
    public void setCalledMethod(String calledMethod) { this.calledMethod = calledMethod; }

    public Map<String, Object> getMethodProperties() { return methodProperties; }
    public void setMethodProperties(Map<String, Object> methodProperties) { this.methodProperties = methodProperties; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
