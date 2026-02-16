package com.example.pricewatch.service;


import com.example.pricewatch.domain.model.User;
import com.example.pricewatch.domain.repository.UserRepository;
import com.example.pricewatch.dto.RegistUserDto;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    //회원가입 , 로그인은 추후 springsecurity로
    private final UserRepository userRepository;

    public User get(Long id){
        return userRepository.findById(id).orElseThrow(()->new RuntimeException("user가 없습니다"));
    }

}