package com.IT_market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.mindrot.jbcrypt.BCrypt;


@SpringBootApplication
public class ItMarketApplication {

	public static void main(String[] args) {
		SpringApplication.run(ItMarketApplication.class, args);
	}
        
        public PasswordEncoder passwordEncoder() {
            return new PasswordEncoder() {
                public String encode(CharSequence rawPassword) {
                    return BCrypt.hashpw(rawPassword.toString(), BCrypt.gensalt());
                }

                public boolean matches(CharSequence rawPassword, String encodedPassword) {
                    return BCrypt.checkpw(rawPassword.toString(), encodedPassword);
                }
            };

    } 
}
