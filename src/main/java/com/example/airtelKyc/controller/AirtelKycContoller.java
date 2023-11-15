package com.example.airtelKyc.controller;

import com.example.airtelKyc.model.OauthRequestBody;
import com.example.airtelKyc.model.UserEnquiryResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
@RestController
@RequestMapping("/api")
public class AirtelKycContoller {
    @Autowired
    private RestTemplate restTemplate;

    @Value("${client.id}")
    private String clientId;

    @Value("${client.secret}")
    private String clientSecret;

    @GetMapping("/kyc")
    public ResponseEntity<String> userEnquiry(@RequestParam(name = "msisdn") String msisdn) {
        Gson gson = new Gson();
        // Obtain OAuth token
        String token = getOAuthToken(clientId, clientSecret);

        // Use the token to call another API
        String apiUrl = "https://openapiuat.airtel.africa/standard/v1/users/";
        apiUrl = apiUrl + msisdn;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("X-Country", "UG");
        headers.set("X-Currency", "UGX");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
        JsonObject jsonObject = gson.fromJson(response.getBody(), JsonObject.class);

        String firstName = jsonObject.getAsJsonObject("data").get("first_name").getAsString();
        String lastName = jsonObject.getAsJsonObject("data").get("last_name").getAsString();
        String fullName = firstName + " " + lastName;
        String registrationStatus = jsonObject.getAsJsonObject("data").getAsJsonObject("registration").get("status").getAsString();

        UserEnquiryResponse userEnquiryResponse = new UserEnquiryResponse();
        userEnquiryResponse.setFull_name(fullName);
        userEnquiryResponse.setRegistration_status(registrationStatus);
        String responseJson = gson.toJson(userEnquiryResponse);

        return new ResponseEntity<>(responseJson, HttpStatus.OK);
    }

    private String getOAuthToken(String clientId, String clientSecret) {
        // Call OAuth service to obtain a token
        String oauthApiUrl = "https://openapiuat.airtel.africa/auth/oauth2/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        OauthRequestBody oauthRequestBody = new OauthRequestBody();
        oauthRequestBody.setClient_id(clientId);
        oauthRequestBody.setClient_secret(clientSecret);
        oauthRequestBody.setGrant_type("client_credentials");

        Gson gson = new Gson();
        String requestBody = gson.toJson(oauthRequestBody);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(oauthApiUrl, request, Map.class);

        // Extract the access token from the response
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().get("access_token").toString();
        } else {
            throw new RuntimeException("Failed to obtain OAuth token");
        }
    }
}
