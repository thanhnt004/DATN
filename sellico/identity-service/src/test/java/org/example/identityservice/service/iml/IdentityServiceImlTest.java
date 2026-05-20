package org.example.identityservice.service.iml;

import org.example.identityservice.dto.request.BuyerRegisterRequest;
import org.example.identityservice.exception.AuthErrorCode;
import org.example.identityservice.exception.KeycloakErrorNormalizer;
import org.example.identityservice.exception.KeycloakException;
import org.example.identityservice.repository.httpclient.UserClient;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class IdentityServiceImlTest {


}

