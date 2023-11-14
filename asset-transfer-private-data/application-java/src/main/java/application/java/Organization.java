package application.java;

import java.util.Map;

public class Organization {
    private String name;
    private Map<String, String> Attributes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getAttributes() {
        return Attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        Attributes = attributes;
    }
}
