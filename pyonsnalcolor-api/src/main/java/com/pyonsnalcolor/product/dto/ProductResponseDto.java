package com.pyonsnalcolor.product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.pyonsnalcolor.product.enumtype.StoreType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@SuperBuilder
@ToString
@Getter
@NoArgsConstructor
public class ProductResponseDto {
    private String id;
    private StoreType storeType;
    private String image;
    private String name;
    private String price;
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedTime;
    private String description;
}
