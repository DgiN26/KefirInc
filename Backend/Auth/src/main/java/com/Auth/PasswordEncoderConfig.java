package com.Auth;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoderConfig.PasswordEncoder customPasswordEncoder() {
        return new PasswordEncoder();
    }

    public static class PasswordEncoder {
        public String encode(String rawPassword) {
            return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        }

        public boolean matches(String rawPassword, String encodedPassword) {
            try {
                return BCrypt.checkpw(rawPassword, encodedPassword);
            } catch (Exception e) {
                return false;
            }
        }
    }
}