package application.java;

import java.util.Map;

public class Organization {
    private String name;
    private Map<String, String> attributes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getGLN(){
        return attributes.get("GLN");
    }
}
