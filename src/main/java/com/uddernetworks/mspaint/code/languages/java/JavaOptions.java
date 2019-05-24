package com.uddernetworks.mspaint.code.languages.java;

import java.util.Arrays;

public enum JavaOptions {
    INPUT_DIRECTORY("inputDirectory", true),
    HIGHLIGHT_DIRECTORY("highlightDirectory", true),
    MAIN("classLocation", true),
    JAR("jarFile", true),
    CLASS_OUTPUT("classOutput", true),
    EXECUTE("execute", true),
    LIBRARY_LOCATION("libraryLocation", false),
    OTHER_LOCATION("otherLocation", false);

    private String name;
    private boolean required;

    JavaOptions(String name, boolean required) {
        this.name = name;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public static JavaOptions fromName(String name) {
        System.out.println("name = " + name);
        return Arrays.stream(values()).filter(option -> option.name.equalsIgnoreCase(name)).findFirst().orElseThrow(() -> new EnumConstantNotPresentException(JavaOptions.class, name + "|shit"));
    }
}