package com.techforge.erp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String company;
}

