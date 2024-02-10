package com.gil.pdfscrapper.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CheckpointFileDto {

    @JsonProperty(defaultValue = "0")
    private int checkpoint;

//    @
//    private String date;

}
