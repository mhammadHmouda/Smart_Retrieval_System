package edu.najah.stu.ir.project.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeoPoint(@JsonProperty("lat") double latitude, @JsonProperty("lon") double longitude) { }