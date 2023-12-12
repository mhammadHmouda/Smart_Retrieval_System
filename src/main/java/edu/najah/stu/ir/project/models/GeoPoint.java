package edu.najah.stu.ir.project.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoPoint {

    @JsonProperty("lat")
    private double latitude;

    @JsonProperty("lon")
    private double longitude;
}
