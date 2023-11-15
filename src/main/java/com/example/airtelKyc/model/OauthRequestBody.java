package com.example.airtelKyc.model;

import lombok.Data;

@Data
public class OauthRequestBody {
    private String client_id;
    private String client_secret;
    private String grant_type;
}
