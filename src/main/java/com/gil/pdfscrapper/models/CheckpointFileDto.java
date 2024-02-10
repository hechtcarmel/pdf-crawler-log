package com.gil.pdfscrapper.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

@NoArgsConstructor
@AllArgsConstructor
@Data
@RegisterReflectionForBinding(CheckpointFileDto.class)
public class CheckpointFileDto {

    @JsonProperty(defaultValue = "0")
    private int checkpoint;

//    @
//    private String date;

}
