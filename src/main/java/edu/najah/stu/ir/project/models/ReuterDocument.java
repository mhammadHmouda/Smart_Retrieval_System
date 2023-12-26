package edu.najah.stu.ir.project.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReuterDocument {
    private long id;

    private String title;

    private String content;

    private String status;

    @JsonProperty("geo_point")
    private GeoPoint geoPoint;

    @JsonProperty("publication_date")
    private String date;

    private List<Author> authors;

    @JsonProperty("temporal_expressions")
    private List<TemporalExpression> temporalExpressions;

    @JsonProperty("geo_references")
    private List<GeoReference> geoReferences;
}