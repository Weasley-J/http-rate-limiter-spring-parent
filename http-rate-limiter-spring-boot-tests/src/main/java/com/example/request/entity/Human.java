package com.example.request.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Enter the description of this class here
 *
 * @author weasley
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Human implements Serializable {
    private Long id;
    private String name;
    private Integer age;
    private Integer sex;
    private String address;
    private String ext;
}
