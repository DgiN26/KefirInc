package com.example.sklad;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Transactional
@Service
public class ServisSkld {
    private final SkladRep userRepository;

    public ServisSkld(SkladRep userRepository) {
        this.userRepository = userRepository;
    }

}
