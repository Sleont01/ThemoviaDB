
package com.example.ThemovieDB.Controller;


import com.example.ThemovieDB.Service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    
// @PostMapping("/iniciar-sesion")
//    public String iniciarSesion(
//            @RequestParam("usuario") String usuario,
//            @RequestParam("contrasena") String contrasena,
//            Model model,
//            HttpSession session) {
//
//        String sessionId = autenticacionService.autenticarConServicioExterno(usuario, contrasena);    
//        if (sessionId != null) {
//            session.setAttribute("session_id", sessionId);
//            return "vista";
//        } else {
//            model.addAttribute("error", "Usuario o contraseña incorrecta");
//            return "Login";
//        }
//    }
    
    @PostMapping("/iniciar-sesion")
public String iniciarSesion(
    @RequestParam("usuario") String usuario,
    @RequestParam("contrasena") String contrasena,
    Model model,
    HttpSession session) {
    
    String sessionId = autenticacionService.autenticarConServicioExterno(usuario, contrasena);
    
    if (sessionId != null) {
      
        RestTemplate restTemplate = new RestTemplate();
        
        
        try {
         
           
            Integer accountId = autenticacionService.obtenerAccountId(sessionId);
            String url = "https://api.themoviedb.org/3/authentication/session/" + accountId + "/rated/movies";
            
           
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Map.class
            );
            
         
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("results")) {
                List<Map<String, Object>> peliculas = (List<Map<String, Object>>) responseBody.get("results");
                model.addAttribute("peliculas", peliculas);
            }
            
            session.setAttribute("session_id", sessionId);
            session.setAttribute("account_id", accountId);
            return "vista"; 
            
        } catch (Exception e) {
            session.setAttribute("session_id", sessionId);
            model.addAttribute("error", "Sesión iniciada pero error al cargar películas");
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
    
   
}
