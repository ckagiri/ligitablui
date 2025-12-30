package com.ligitabl.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Team {
    private String code;
    private String name;
    private String crestUrl;
}
