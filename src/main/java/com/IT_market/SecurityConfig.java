
package com.IT_market;


package com.onlinemkt.sample;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;


@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) // allow all requests
            .csrf(csrf -> csrf.disable()); // disable CSRF if you want forms to work without login

        return http.build();
    }
           // public UserDetailsService userDetailsService() {
             //   UserDetails user = User.builder()
               //         .username("Tracy") 
                 //       .password(passwordEncoder().encode("12345678")) // Replace with your desired password
                   //     .roles("USER")  // Replace with your desired role
                     //   .build();
                //return new InMemoryUserDetailsManager(user);
            //}
    
            //@Bean
    //public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      //  http
        //    .authorizeHttpRequests()
          //      .requestMatchers("/checkout", "/process-payment","/dashboard","/admin", "/payment-status").authenticated() // only logged-in users
            //    .anyRequest().permitAll() // public pages like /login, /register, /cart
            //.and()
            //.formLogin()
                
              //  .permitAll()
            //.and()
            //.logout()
              //  .permitAll()
            //.and()
            //.csrf(); // keep CSRF enabled for forms

        //return http.build();
    //}

           
    }

