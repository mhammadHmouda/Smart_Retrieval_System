package edu.najah.stu.ir.project.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReuterDocument {
    private long id;
    private String title;
    private String content;
    private String status;
    private String date;
    private long epoch;
    private GeoPoint geoPoint;
    private List<Author> authors;
    private List<String> temporalExpressions;
    private List<String> geoReferences;
}