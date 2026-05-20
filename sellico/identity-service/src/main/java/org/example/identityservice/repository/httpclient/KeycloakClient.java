package org.example.identityservice.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "keycloak-client", url = "${app.keycloak.server-url}")
public interface KeycloakClient {

    @PostMapping(value = "/realms/${app.keycloak.realm}/protocol/openid-connect/logout",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    void logout(@RequestBody MultiValueMap<String, String> params);

    @PostMapping(value = "/realms/${app.keycloak.realm}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    Map<String, Object> exchangeToken(@RequestBody MultiValueMap<String, String> params);
}
