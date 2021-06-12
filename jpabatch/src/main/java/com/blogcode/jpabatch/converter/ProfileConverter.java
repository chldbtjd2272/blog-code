package com.blogcode.jpabatch.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class ProfileConverter implements AttributeConverter<Profile, String> {
    private static final String PATH = "/image";
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Profile attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public Profile convertToEntityAttribute(String dbData) {
        try {
            Profile profile = mapper.readValue(dbData, Profile.class);
            profile.setPath(PATH);
            return profile;
        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }
    }
}
