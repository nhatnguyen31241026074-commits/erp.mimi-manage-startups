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

    @Override
    public String toString() {
        if (this.name != null && !this.name.isEmpty()) return this.name;
        if (this.email != null && !this.email.isEmpty()) return this.email;
        return this.id != null ? this.id : "(No clients found)";
    }
}
