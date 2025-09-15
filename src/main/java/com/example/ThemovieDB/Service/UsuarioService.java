
package com.example.ThemovieDB.Service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
  
    
@Service
public class UsuarioService {
    private final String apiKey = "8fce607166c57f7d780af4d5db77982a"; 
    private final RestTemplate restTemplate = new RestTemplate();
    
    public String getApiKey() {
        return apiKey;
    }

    public String autenticarConServicioExterno(String usuario, String contrasena) {

        try {
            ResponseEntity<Map> tokenResponse = restTemplate.getForEntity(
                    "https://api.themoviedb.org/3/authentication/token/new?api_key=" + apiKey,
                    Map.class
            );

            if (tokenResponse.getStatusCode() != HttpStatus.OK) return null;

            String requestToken = (String) tokenResponse.getBody().get("request_token");

            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", usuario);
            loginRequest.put("password", contrasena);
            loginRequest.put("request_token", requestToken);

            ResponseEntity<Map> validateResponse = restTemplate.postForEntity(
                    "https://api.themoviedb.org/3/authentication/token/validate_with_login?api_key=" + apiKey,
                    loginRequest,
                    Map.class
            );

            if (validateResponse.getStatusCode() != HttpStatus.OK) return null;

            Map<String, String> sessionRequest = new HashMap<>();
            sessionRequest.put("request_token", requestToken);

            ResponseEntity<Map> sessionResponse = restTemplate.postForEntity(
                    "https://api.themoviedb.org/3/authentication/session/new?api_key=" + apiKey,
                    sessionRequest,
                    Map.class
            );

            if (sessionResponse.getStatusCode() == HttpStatus.OK) {
                return (String) sessionResponse.getBody().get("session_id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    
    public Integer obtenerAccountId(String sessionId) {
    try {
        String url = "https://api.themoviedb.org/3/account?api_key=" + apiKey + "&session_id=" + sessionId;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return (Integer) response.getBody().get("id");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
    return null;
}

    
}
