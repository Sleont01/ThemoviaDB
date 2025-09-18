
package com.example.ThemovieDB.Controller;


import com.example.ThemovieDB.Service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;


@Controller
@RequestMapping("usuario")
public class UsuarioController {
    
    @Autowired
    private UsuarioService autenticacionService;
   
    
    @GetMapping
    public String Index(Model model){
        
        return "vista";
        
        
    }
    
    @PostMapping("/iniciar-sesion")
public String iniciarSesion(
        @RequestParam("usuario") String usuario,
        @RequestParam("contrasena") String contrasena,
        Model model,
        HttpSession session) {

    String sessionId = autenticacionService.autenticarConServicioExterno(usuario, contrasena);
    RestTemplate restTemplate = new RestTemplate();

    if (sessionId != null) {
        try {
            
            session.setAttribute("session_id", sessionId);
            String urlAccount = "https://api.themoviedb.org/3/account"
                    + "?api_key=" + autenticacionService.getApiKey()
                    + "&session_id=" + sessionId;

            Map<String, Object> cuenta = restTemplate.getForObject(urlAccount, Map.class);
            if (cuenta != null && cuenta.get("id") != null) {
                session.setAttribute("account_id", (Integer) cuenta.get("id"));
                model.addAttribute("cuenta", cuenta); 
            }
           
            String url = "https://api.themoviedb.org/3/movie/popular"
                    + "?api_key=" + autenticacionService.getApiKey()
                    + "&language=es-MX&page=1";

     
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("results")) {
                List<Map<String, Object>> peliculas = (List<Map<String, Object>>) responseBody.get("results");
                model.addAttribute("peliculas", peliculas);
            }

            session.setAttribute("session_id", sessionId);
            return "vista"; 

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("session_id", sessionId);
            model.addAttribute("error", "Sesión iniciada pero error al cargar películas populares");
            return "vista"; 
        }
    } else {
        model.addAttribute("error", "Usuario o contraseña incorrecta");
        return "Login"; 
    }
}



    
    @PostMapping("/logout")
    public String Fuerasesion(Model model){
        
    return "Login";
    }
    
    @PostMapping("/crear-sesion")
    public String crearSesion(
            @RequestParam("usuario") String usuario,
            @RequestParam("contrasena") String contrasena,
            @RequestParam("repetirContrasena") String repetirContrasena,
            @RequestParam("email") String email,
            Model model,
            HttpSession session) {

        RestTemplate restTemplate = new RestTemplate();

        // Validar que las contraseñas coincidan
        if (!contrasena.equals(repetirContrasena)) {
            model.addAttribute("error", "Las contraseñas no coinciden");
            return "Login";
        }

        try {
            // Paso 1: Obtener request_token
            String tokenUrl = "https://api.themoviedb.org/3/authentication/token/new?api_key="
                    + autenticacionService.getApiKey();
            Map<String, Object> tokenResponse = restTemplate.getForObject(tokenUrl, Map.class);
            String requestToken = (String) tokenResponse.get("request_token");

            // Paso 2: Validar request_token con login
            String validateUrl = "https://api.themoviedb.org/3/authentication/token/validate_with_login?api_key="
                    + autenticacionService.getApiKey();
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", usuario);
            loginRequest.put("password", contrasena);
            loginRequest.put("request_token", requestToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> validateEntity = new HttpEntity<>(loginRequest, headers);
            restTemplate.postForEntity(validateUrl, validateEntity, Map.class);

            // Paso 3: Crear la sesión
            String sessionUrl = "https://api.themoviedb.org/3/authentication/session/new?api_key="
                    + autenticacionService.getApiKey();
            Map<String, String> sessionRequest = new HashMap<>();
            sessionRequest.put("request_token", requestToken);

            HttpEntity<Map<String, String>> sessionEntity = new HttpEntity<>(sessionRequest, headers);
            ResponseEntity<Map> sessionResponse = restTemplate.postForEntity(sessionUrl, sessionEntity, Map.class);

            String sessionId = (String) sessionResponse.getBody().get("session_id");

            if (sessionId != null) {
                // Guardar en la sesión del servidor
                session.setAttribute("session_id", sessionId);

                // Obtener accountId
                Integer accountId = autenticacionService.obtenerAccountId(sessionId);
                session.setAttribute("account_id", accountId);

                // Paso 4: Obtener películas calificadas
                String url = "https://api.themoviedb.org/3/account/" + accountId + "/rated/movies"
                        + "?api_key=" + autenticacionService.getApiKey()
                        + "&session_id=" + sessionId;

                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                Map<String, Object> responseBody = response.getBody();

                if (responseBody != null && responseBody.containsKey("results")) {
                    List<Map<String, Object>> peliculas = (List<Map<String, Object>>) responseBody.get("results");
                    model.addAttribute("peliculas", peliculas);
                }

                model.addAttribute("mensaje", "Sesión creada correctamente con TMDB");
                return "vista"; // tu vista con películas

            } else {
                model.addAttribute("error", "No se pudo crear la sesión en TMDB");
                return "Login";
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error al crear sesión");
            return "Login";
        }
    }
    
    
    @GetMapping("/detalle-pelicula/{movieId}")
    @ResponseBody
    public Map<String, Object> detallePelicula(@PathVariable("movieId") Integer movieId) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.themoviedb.org/3/movie/" + movieId
                + "?api_key=" + autenticacionService.getApiKey()
                + "&language=es-MX";

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return response.getBody(); 
    }
    
    @GetMapping("/perfil/detalles")
public String perfilDetalles(Model model, HttpSession session) {
    String sessionId = (String) session.getAttribute("session_id");
    Integer accountId = (Integer) session.getAttribute("account_id");
    if (sessionId == null || accountId == null) {
        return "redirect:/Login";
    }

    RestTemplate restTemplate = new RestTemplate();

    String urlAccount = "https://api.themoviedb.org/3/account/" + accountId
            + "?api_key=" + autenticacionService.getApiKey()
            + "&session_id=" + sessionId;

    Map<String, Object> cuenta = restTemplate.getForObject(urlAccount, Map.class);
    model.addAttribute("cuenta", cuenta);

    return "perfil-detalles";
}

@GetMapping("/perfil/favoritas")
public String perfilFavoritas(Model model, HttpSession session) {
    String sessionId = (String) session.getAttribute("session_id");
    Integer accountId = (Integer) session.getAttribute("account_id");
    if (sessionId == null || accountId == null) {
        return "redirect:/Login";
    }

    RestTemplate restTemplate = new RestTemplate();

    // Obtener películas favoritas
    String urlFav = "https://api.themoviedb.org/3/account/" + accountId + "/favorite/movies"
            + "?api_key=" + autenticacionService.getApiKey()
            + "&session_id=" + sessionId
            + "&language=es-MX&page=1";

    Map<String, Object> responseFav = restTemplate.getForObject(urlFav, Map.class);
    List<Map<String, Object>> peliculasFav = (List<Map<String, Object>>) responseFav.get("results");
    model.addAttribute("peliculasFav", peliculasFav);

    return "perfil-favoritas";
}

      
}
