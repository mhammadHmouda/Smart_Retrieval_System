package edu.najah.stu.ir.project.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Author(@JsonProperty("first_name") String firstName, @JsonProperty("last_name") String lastName) { }